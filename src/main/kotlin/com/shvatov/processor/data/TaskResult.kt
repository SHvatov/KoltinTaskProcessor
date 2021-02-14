package com.shvatov.processor.data

/**
 * @author shvatov
 */
data class TaskResult<R : Any>(
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
    val failureCause: Throwable? = null
) {
    /**
     * Defines, whether timeout has occurred before the end of the task
     * execution or not.
     */
    val isTimeout: Boolean = completionState == TaskCompletionState.TIMEOUT

    /**
     * Defines, whether exception has occurred before the end of the task
     * execution or not.
     */
    val isFailure: Boolean = completionState == TaskCompletionState.FAILURE

    /**
     * Execution of the task has been completed successfully.
     */
    val isSuccess: Boolean = completionState == TaskCompletionState.SUCCESS
}