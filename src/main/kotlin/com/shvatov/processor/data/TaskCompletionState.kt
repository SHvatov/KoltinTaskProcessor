package com.shvatov.processor.data

/**
 * Determines the state of the [TaskResult] after a [Task],
 * which produces this result, has been processed by the processor.
 * @author shvatov
 */
enum class TaskCompletionState {
    /**
     * Task has successfully been processed by the processor.
     */
    SUCCESS,

    /**
     * Task has failed and probably no result is stored within
     * [TaskResult] instance, only the exception which caused the failure.
     */
    FAILURE,

    /**
     * Task has exceeded the timeout. If provided [Task] supports cancellation,
     * then result may be null.
     */
    TIMEOUT
}