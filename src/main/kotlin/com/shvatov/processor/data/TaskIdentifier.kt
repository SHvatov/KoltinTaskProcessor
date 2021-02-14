package com.shvatov.processor.data

import java.util.UUID

/**
 * @author shvatov
 */
data class TaskIdentifier(
    /**
     * Unique id of the processor, which is used to
     * process the following task,
     */
    val processorId: UUID,

    /**
     * Unique id of the message, assigned by the processor.
     * Must be unique within processor.
     */
    val taskId: UUID
)