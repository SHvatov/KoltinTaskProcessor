package com.shvatov.processor.config

import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * @author shvatov
 */
data class ProcessorConfiguration(
    /**
     * Defines, whether a dispatcher from parent coroutine should be used
     * or a new one must be created.
     */
    val useParentDispatcher: Boolean = true,

    /**
     * Defines the size of the pool to be used as a Dispatcher
     * if [useParentDispatcher] is set to true.
     */
    val threadPoolSize: Int = if (useParentDispatcher) 0 else POOL_SIZE,

    /**
     * Exception handler for uncaught exceptions during the processing of the task.
     */
    val exceptionHandler: CoroutineExceptionHandler,

    /**
     * Defines, whether processor should fail if an exception occurs in one the
     * sub-processor, or it may be just logged and skipped.
     */
    val failOnException: Boolean = false,

    /**
     * Number of the sub-processors to be used for task dispatching.
     */
    val subProcessorsNumber: Int = SUB_PROCESSORS_NUMBER,

    /**
     * Delay, before next attempt to dispatch a task to one of the
     * sub-processors.
     */
    val dispatchFailureDelay: Long = DISPATCH_FAILURE_DELAY,

    /**
     * Number of attempts, that may fail before dispatching a task to sub-processor.
     */
    val dispatchAttempts: Int = DISPATCH_ATTEMPTS_NUMBER,

    /**
     * Task execution timeout.
     */
    val taskExecutionTimeout: Long = TASK_EXECUTION_TIMEOUT
) {
    init {
        require(threadPoolSize in 1..POOL_SIZE)
        require(subProcessorsNumber in 1..SUB_PROCESSORS_NUMBER)
        require(dispatchFailureDelay in 1..DISPATCH_FAILURE_DELAY)
        require(dispatchAttempts in 1..DISPATCH_ATTEMPTS_NUMBER)
        require(taskExecutionTimeout in 1..TASK_EXECUTION_TIMEOUT)
    }

    companion object {
        const val POOL_SIZE = 3

        const val SUB_PROCESSORS_NUMBER = 3

        const val DISPATCH_FAILURE_DELAY = 10000L

        const val TASK_EXECUTION_TIMEOUT = Long.MAX_VALUE

        const val DISPATCH_ATTEMPTS_NUMBER = 10
    }
}