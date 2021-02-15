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
    val threadPoolSize: Int = if (useParentDispatcher) 0 else MAX_POOL_SIZE,

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
    val subProcessorsNumber: Int = MAX_SUB_PROCESSORS_NUMBER,

    /**
     * Delay, before next attempt to dispatch a task to one of the
     * sub-processors.
     */
    val dispatchFailureDelay: Long = MAX_DISPATCH_FAILURE_DELAY,

    /**
     * Number of attempts, that may fail before dispatching a task to sub-processor.
     */
    val dispatchAttempts: Int = MAX_DISPATCH_ATTEMPTS_NUMBER,

    /**
     * Task execution timeout.
     */
    val taskExecutionTimeout: Long = MAX_TASK_EXECUTION_TIMEOUT,

    /**
     * Capacity of the output channel.
     */
    val outputChannelCapacity: Int = MAX_OUTPUT_CHANNEL_CAPACITY,

    /**
     * Capacity of the dispatcher. Number of the tasks that can be stored for the further
     * processing before dispatcher suspends execution of the calling thread.
     */
    val dispatcherChannelCapacity: Int = MAX_DISPATCHER_CHANNEL_CAPACITY
) {
    init {
        if (useParentDispatcher) require(threadPoolSize == 0)
        else require(threadPoolSize in 1..MAX_POOL_SIZE)
        require(subProcessorsNumber in 1..MAX_SUB_PROCESSORS_NUMBER)
        require(dispatchFailureDelay in 1..MAX_DISPATCH_FAILURE_DELAY)
        require(dispatchAttempts in 1..MAX_DISPATCH_ATTEMPTS_NUMBER)
        require(taskExecutionTimeout in 1..MAX_TASK_EXECUTION_TIMEOUT)
        require(outputChannelCapacity in 1..MAX_OUTPUT_CHANNEL_CAPACITY)
        require(dispatcherChannelCapacity in 1..MAX_DISPATCHER_CHANNEL_CAPACITY)
    }

    companion object {
        const val MAX_POOL_SIZE = 10

        const val MAX_SUB_PROCESSORS_NUMBER = 1000

        const val MAX_DISPATCH_FAILURE_DELAY = 10000L

        const val MAX_TASK_EXECUTION_TIMEOUT = Long.MAX_VALUE

        const val MAX_DISPATCH_ATTEMPTS_NUMBER = 10

        const val MAX_OUTPUT_CHANNEL_CAPACITY = 100

        const val MAX_DISPATCHER_CHANNEL_CAPACITY = 100
    }
}