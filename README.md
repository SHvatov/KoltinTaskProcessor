# KotlinTaskProcessor

## Description

This module provides `TaskProcessor` interface and its implementation based on Kotlin Coroutines and
channels. Main idea of this processor is to use lightweight coroutines in order to implement a
scalable processor with its own sub-processors, that are in charge of executing tasks provided to
the processor itself.

## Usage

`TaskProcessor` can be used as follwoing:

```kotlin
 // first obtain a reference to processor's output channel
val outputChannel = processor.outputChannel

// then launch processor in another coroutine to prevent blocking
launch {
    processor.use {
        tasks.forEach {
            submit(it)
        }
    }
}

// process the results in a separate coroutine
val results = mutableListOf<TaskResult<TestResult>>()
outputChannel.consumeEach {
    results.add(it)
    println("Consumed following result: $it")
}
```

`TaskProcessor` supports `use` extension function, which closes the processor after the execution of
the provided block.

`TaskProcessor#close` method is also a suspended one. It suspends coroutine it is executed on until
an inner dispatcher has sent all the provided tasks to the sub-processors and all of them have
finished their jobs. After that, `outputChannel` is closed, notifying others that execution of the
processor has finished. Because of that, access to the
`outputChannel` in the same coroutine can lead to an infinite loop.

## Configuration

`TaskProcessor` can be configured in different ways using `TaskProcessorConfiguration`:

```kotlin
data class TaskProcessorConfiguration(
    /**
     * Defines, whether a dispatcher from parent coroutine should be used
     * or a new one must be created.
     */
    val useParentDispatcher: Boolean = true,

    /**
     * Defines the size of the pool to be used as a Dispatcher
     * if [useParentDispatcher] is set to true.
     */
    val threadPoolSize: Int = if (useParentDispatcher) 0 else DEFAULT_POOL_SIZE,

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
    val subProcessorsNumber: Int = DEFAULT_SUB_PROCESSORS_NUMBER,

    /**
     * Delay, before next attempt to dispatch a task to one of the
     * sub-processors.
     */
    val dispatchFailureDelay: Long = DEFAULT_DISPATCH_FAILURE_DELAY,

    /**
     * Number of attempts, that may fail before dispatching a task to sub-processor.
     */
    val dispatchAttempts: Int = DEFAULT_DISPATCH_ATTEMPTS_NUMBER,

    /**
     * Task execution timeout.
     */
    val taskExecutionTimeout: Long = DEFAULT_TASK_EXECUTION_TIMEOUT,

    /**
     * Capacity of the output channel.
     */
    val outputChannelCapacity: Int = DEFAULT_OUTPUT_CHANNEL_CAPACITY,

    /**
     * Capacity of the dispatcher. Number of the tasks that can be stored for the further
     * processing before dispatcher suspends execution of the calling thread.
     */
    val dispatcherChannelCapacity: Int = DEFAULT_DISPATCHER_CHANNEL_CAPACITY
)
```
