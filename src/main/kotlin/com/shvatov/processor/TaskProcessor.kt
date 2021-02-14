package com.shvatov.processor

import com.shvatov.processor.config.ProcessorConfiguration
import com.shvatov.processor.data.Task
import com.shvatov.processor.data.TaskIdentifier
import com.shvatov.processor.data.TaskResult
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.UUID

/**
 * Interface of the basic processor. Each processor is aware of its own
 * coroutine scope and the scope of the parent coroutine if such is present.
 * @author shvatov
 */
interface TaskProcessor<P : Any, R : Any> : CloseableProcessor, CoroutineScopeAware, CoroutineParentScopeAware {
    /**
     * Unique identifier of this processor.
     */
    val processorIdentifier: UUID

    /**
     * Configuration of the processor. For more details - see docs of [ProcessorConfiguration].
     */
    val configuration: ProcessorConfiguration

    /**
     * Channel, which contains the results of the [Task], that have been processed.
     */
    val outputChannel: ReceiveChannel<TaskResult<R>>

    /**
     * Submits a [Task] for the following processing. Suspends the execution
     * until task is sent to the inner dispatcher. Returns a [TaskIdentifier],
     * which can be used to associate a task with its result after the completion
     * of the processing.
     */
    suspend fun submit(task: Task<P, R>): TaskIdentifier
}