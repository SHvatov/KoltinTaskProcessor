package com.shvatov.processor

import com.shvatov.processor.config.ProcessorConfiguration
import com.shvatov.processor.data.Task
import com.shvatov.processor.data.TaskIdentifier
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Interface of the basic processor. Each processor is aware of its own
 * coroutine scope and the scope of the parent coroutine if such is present.
 * @author shvatov
 */
interface Processor<P : Any?, R : Any?> : CoroutineScopeAware, CoroutineParentScopeAware {
    /**
     * Configuration of the processor. For more details - see docs of [ProcessorConfiguration].
     */
    val configuration: ProcessorConfiguration

    /**
     * Channel, which contains the results of the [Task], that have been processed.
     */
    val outputChannel: ReceiveChannel<Result<R>>

    /**
     * Submits a [Task] for the following processing. Suspends the execution
     * until task is sent to the inner dispatcher. Returns a [TaskIdentifier],
     * which can be used to associate a task with its result after the completion
     * of the processing.
     */
    suspend fun submit(task: Task<P, R>): TaskIdentifier
}