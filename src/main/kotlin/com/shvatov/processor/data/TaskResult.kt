package com.shvatov.processor.data

/**
 * @author shvatov
 */
data class TaskResult<R : Any?>(
    /**
     * Associates an instance of the [TaskResult] with the initial task.
     */
    val identifier: TaskIdentifier,

    /**
     * Result of the [Task] being processed by the processor.
     */
    val result: R? = null,

    /**
     * Defines how the task has been processed - successfully or not.
     */
    val completionState: TaskCompletionState = TaskCompletionState.SUCCESS,

    /**
     * If task has failed, then an exception, which caused
     * the failure is stored here.
     */
    val failureCause: Throwable? = null,
)