package com.shvatov.processor.impl

import com.shvatov.processor.config.ProcessorConfiguration
import com.shvatov.processor.data.Task
import com.shvatov.processor.data.TaskResult
import com.shvatov.processor.use
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

/**
 * Tests for [TaskProcessorImpl] with some examples of the correct usage.
 * @author shvatov
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal class TaskProcessorImplTest {
    @Test
    fun `successful processing of multiple tasks - use parent dispatcher`() = runBlocking<Unit> {
        val processor = TaskProcessorImpl<TestPayload, TestResult>(
            ProcessorConfiguration(
                exceptionHandler = CoroutineExceptionHandler { _, ex -> println(ex.stackTrace) },
                dispatchFailureDelay = 1000L,
                taskExecutionTimeout = 1500L,
                subProcessorsNumber = 3
            ),
            this
        )

        // generate some tasks with estimated time = 100 second
        // if run sequentially
        val tasks = (1..100).map {
            val payload = TestPayload(it)
            Task(payload) {
                delay(1000)
                TestResult(it.id.hashCode().toString())
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

            assertTrue { results.all { it.isSuccess } }
            assertTrue { results.all { it.result?.hash != null } }
        }

        println("Time spent processing: $timeSpent ns")
        assertTrue { timeSpent < 100e9 }
    }

    @Test
    fun `successful processing of multiple tasks - use new dispatcher`() = runBlocking<Unit> {
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

        // generate some tasks with estimated time = 100 second
        // if run sequentially
        val tasks = (1..100).map {
            val payload = TestPayload(it)
            Task(payload) {
                delay(1000)
                TestResult(it.id.hashCode().toString())
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

            assertTrue { results.all { it.isSuccess } }
            assertTrue { results.all { it.result?.hash != null } }
        }

        println("Time spent processing: $timeSpent ns")
        assertTrue { timeSpent < 100e9 }
    }
}