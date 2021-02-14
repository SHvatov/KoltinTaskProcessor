package com.shvatov.processor.impl

import com.shvatov.processor.TaskProcessor
import com.shvatov.processor.config.ProcessorConfiguration
import com.shvatov.processor.data.Task
import com.shvatov.processor.data.TaskCompletionState
import com.shvatov.processor.data.TaskIdentifier
import com.shvatov.processor.data.TaskResult
import com.shvatov.processor.impl.TaskProcessorImpl.TaskSubProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [TaskProcessor]. Plays role of some kind of dispatcher, while
 * [TaskSubProcessor] instances are responsible for the task processing.
 * @author shvatov
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class TaskProcessorImpl<P : Any, R : Any>(
    override val configuration: ProcessorConfiguration,
    override val parentScope: CoroutineScope? = null
) : TaskProcessor<P, R> {
    override val processorIdentifier: UUID = UUID.randomUUID()

    override val outputChannel: ReceiveChannel<TaskResult<R>> =
        Channel(configuration.outputChannelCapacity)

    override val scope: CoroutineScope =
        prepareCoroutineScope(processorIdentifier, parentScope, configuration)

    /**
     * List of children sub-processors that are being managed by this processor.
     */
    private val subProcessors = mutableListOf<TaskSubProcessor>()

    /**
     * Determines, whether the execution of [dispatcher] has finished and it is safe to close
     * all other [subProcessors] that are idle at the moment.
     */
    private var isDispatcherFinished = CompletableDeferred<Boolean>()

    /**
     * Coroutine, which is responsible for dispatching incoming tasks to the [subProcessors].
     */
    private val dispatcher: SendChannel<InternalTask<P, R>> =
        scope.actor(
            capacity = configuration.dispatcherChannelCapacity,
            start = CoroutineStart.LAZY
        ) {
            consumeEach {
                dispatch(it)
            }.also {
                isDispatcherFinished.complete(true)
            }
        }

    override suspend fun submit(task: Task<P, R>): TaskIdentifier {
        return TaskIdentifier(processorIdentifier, UUID.randomUUID()).also {
            dispatcher.send(InternalTask(it, task))
        }
    }

    override suspend fun close() {
        // close the dispatcher to prevent new tasks from
        // appearing in the channel
        dispatcher.close()

        // wait for the moment when dispatcher has finished
        // processing the messages from the channel - no more
        // tasks to await, so that now we are sure, that all idle
        // sub-processors will remain idle
        isDispatcherFinished.await()

        // close all the sub-processors; after we ensured that dispatcher
        // has finished, we are sure that no ClosedChannelException will occur
        // because there are no tasks to dispatch left, meaning that processors
        // are now processing their final tasks
        subProcessors.forEach { it.processor.close() }
    }

    /**
     * Dispatches [internalTask] to one of the sub-processors, using the following algorithm:
     * - chooses one of the idle SP with the min number of processed tasks
     * - if no idle present && we are able to create a new SP - do so
     * - otherwise waits for defined delay for some SP to free
     * - retry for defined number of times or throw exception
     */
    private suspend fun dispatch(internalTask: InternalTask<P, R>) {
        for (retry in 0 until configuration.dispatchAttempts) {
            var chosenChild = subProcessors
                .filter { it.idle }
                .minByOrNull { it.processedTasks }

            if (chosenChild == null) {
                if (subProcessors.size < configuration.subProcessorsNumber) {
                    chosenChild = TaskSubProcessor()
                    subProcessors.add(chosenChild)
                } else {
                    delay(configuration.dispatchFailureDelay)
                    continue
                }
            }

            chosenChild.processor.send(internalTask)
            return
        }

        throw IllegalStateException("Unable to dispatch an assembly task - out of retries!")
    }

    /**
     * Sub-processor, which is responsible for task execution.
     */
    private inner class TaskSubProcessor {
        /**
         * Defines, whether this sub-processor is busy processing some task or
         * is being idle and does nothing at the moment.
         */
        private var _idle: Boolean = false
        val idle = _idle

        /**
         * Contains the number of tasks this sub-processor has processed during its whole
         * lifetime. Used by the processor's task dispatching algorithm.
         */
        private var _processedTasks: Long = 0L
        val processedTasks = _processedTasks

        /**
         * Child actor which is responsible for the procession of the tasks that are sent
         * to its channel. Exists while there are some tasks to process in the [processor]
         * and until [processor] is closed by the parent.
         */
        val processor: SendChannel<InternalTask<P, R>> =
            scope.actor(start = CoroutineStart.LAZY) {
                consumeEach {
                    _idle = false

                    val result = withTimeoutOrNull(configuration.taskExecutionTimeout) {
                        processTask(it.taskIdentifier, it.task)
                    } ?: TaskResult(identifier = it.taskIdentifier, completionState = TaskCompletionState.TIMEOUT)

                    @Suppress("unchecked_cast")
                    (outputChannel as SendChannel<TaskResult<R>>).send(result)

                    _idle = true
                    _processedTasks += 1
                }
            }

        /**
         * Processes the incoming [task]. Based on the configuration, can either re-throw occurred
         * exception or suppress it.
         */
        private fun CoroutineScope.processTask(identifier: TaskIdentifier, task: Task<P, R>): TaskResult<R> {
            return try {
                val (payload, action) = task
                val procResult = action(payload)
                TaskResult(identifier, procResult)
            } catch (exception: Throwable) {
                if (!configuration.failOnException) {
                    TaskResult(identifier, null, TaskCompletionState.FAILURE, exception)
                } else throw exception
            }
        }
    }

    /**
     * Internal representation of the task with its identifier for the further usage
     * in creation of [TaskResult] object.
     */
    private data class InternalTask<P : Any, R : Any>(
        val taskIdentifier: TaskIdentifier,
        val task: Task<P, R>
    )

    private companion object {
        /**
         * Based on the provided [configuration] and [parentScope] creates a [CoroutineScope]
         * for this processor to use.
         */
        fun prepareCoroutineScope(
            identifier: UUID,
            parentScope: CoroutineScope?,
            configuration: ProcessorConfiguration
        ): CoroutineScope {
            require(!configuration.useParentDispatcher || parentScope != null) {
                "Can't use parent dispatcher when no parent scope is provided"
            }

            val dispatcher = if (!configuration.useParentDispatcher || parentScope == null) {
                Executors
                    .newFixedThreadPool(configuration.threadPoolSize)
                    .asCoroutineDispatcher()
            } else null

            var context: CoroutineContext = CoroutineName("Processor-$identifier")
            dispatcher?.let { context += it }
            parentScope?.let { context += it.coroutineContext }
            context += configuration.exceptionHandler

            return CoroutineScope(context)
        }
    }
}