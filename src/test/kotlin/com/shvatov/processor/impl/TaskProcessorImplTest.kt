package com.shvatov.processor.impl

import com.shvatov.processor.config.ProcessorConfiguration
import com.shvatov.processor.data.Task
import com.shvatov.processor.data.TaskResult
import com.shvatov.processor.use
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

/**
 * Tests for [TaskProcessorImpl] with some examples of the correct usage.
 * @author shvatov
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Timeout(value = 100L, unit = TimeUnit.SECONDS)
internal class TaskProcessorImplTest {
    @Test
    @Timeout(value = 100L, unit = TimeUnit.SECONDS)
    fun `successful processing of multiple tasks - use parent dispatcher`() = runBlocking {
        val processor = TaskProcessorImpl<TestPayload, TestResult>(
            ProcessorConfiguration(
                exceptionHandler = CoroutineExceptionHandler { _, ex -> println(ex.stackTrace) },
                dispatchFailureDelay = 1000L,
                taskExecutionTimeout = 1500L,
                subProcessorsNumber = 3
            ),
            this
        )

        testProcessor(processor, 100)
    }

    @Test
    fun `successful processing of multiple tasks - use new dispatcher with more sub-proc`() = runBlocking {
        val processor = TaskProcessorImpl<TestPayload, TestResult>(
            ProcessorConfiguration(
                exceptionHandler = CoroutineExceptionHandler { _, ex -> println(ex.stackTrace) },
                dispatchFailureDelay = 1000L,
                taskExecutionTimeout = 1500L,
                useParentDispatcher = false,
                threadPoolSize = 5,
                subProcessorsNumber = 100,
                outputChannelCapacity = 40,
                dispatcherChannelCapacity = 25
            ),
            this
        )

        testProcessor(processor, 100)
    }

    @Test
    fun `process with failure - ignore exceptions`() = runBlocking {
        val processor = TaskProcessorImpl<TestPayload, TestResult>(
            ProcessorConfiguration(
                exceptionHandler = CoroutineExceptionHandler { _, ex -> println(ex.stackTrace) },
                dispatchFailureDelay = 1000L,
                taskExecutionTimeout = 1500L,
                useParentDispatcher = false,
                threadPoolSize = 5,
                subProcessorsNumber = 100,
                outputChannelCapacity = 40,
                dispatcherChannelCapacity = 25,
                failOnException = false
            ),
            this
        )

        testProcessor(processor, 100, true)
    }

    @Test
    fun `process with failure - fail on exceptions`() {
        assertThrows<UnsupportedOperationException> {
            runBlocking {
                val processor = TaskProcessorImpl<TestPayload, TestResult>(
                    ProcessorConfiguration(
                        exceptionHandler = CoroutineExceptionHandler { _, ex -> println(ex.stackTrace) },
                        dispatchFailureDelay = 1000L,
                        taskExecutionTimeout = 1500L,
                        useParentDispatcher = false,
                        threadPoolSize = 5,
                        subProcessorsNumber = 100,
                        outputChannelCapacity = 40,
                        dispatcherChannelCapacity = 25,
                        failOnException = true
                    ),
                    this
                )

                testProcessor(processor, 100, true)
            }
        }
    }

    private suspend fun testProcessor(
        processor: TaskProcessorImpl<TestPayload, TestResult>,
        tasksNumber: Int,
        withFailure: Boolean = false
    ) = coroutineScope {
        val tasks = prepareTasks(tasksNumber).apply {
            if (withFailure) {
                removeLast()
                val payload = TestPayload(-1)
                val task = Task<TestPayload, TestResult>(payload) {
                    delay(1000)
                    throw UnsupportedOperationException()
                }
                add(task)
                shuffle()
            }
        }

        val timeSpent = measureNanoTime {
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

            val results = mutableListOf<TaskResult<TestResult>>()
            outputChannel.consumeEach {
                results.add(it)
                println("Consumed following result: $it\n\t${tasks.size - results.size} left")
            }

            if (!withFailure) {
                assertTrue { results.all { it.isSuccess } }
                assertTrue { results.all { it.result?.hash != null } }
            } else {
                assertTrue { results.any { it.isFailure } }
            }
        }

        if (!withFailure) {
            println("Time spent processing: $timeSpent ns")
            assertTrue { timeSpent < tasks.size * 1e9 }
        }
    }

    private fun prepareTasks(n: Int): MutableList<Task<TestPayload, TestResult>> =
        (1..n).map {
            val payload = TestPayload(it)
            Task(payload) { p ->
                delay(1000)
                TestResult(p.id.hashCode().toString())
            }
        }.toMutableList()
}