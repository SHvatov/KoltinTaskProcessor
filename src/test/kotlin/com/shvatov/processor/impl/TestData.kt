package com.shvatov.processor.impl

/**
 * Test payload class.
 * @author shvatov
 */
data class TestPayload(
    val id: Int = 10
)

/**
 * Test result class.
 */
data class TestResult(
    val hash: String
)