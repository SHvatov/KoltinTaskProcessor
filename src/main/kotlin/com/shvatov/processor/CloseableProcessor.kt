package com.shvatov.processor

/**
 * Analog of [AutoCloseable] interface, which contains definition
 * of the close function marked as suspended due to its nature.
 * @author shvatov
 */
interface CloseableProcessor {
    suspend fun close()
}

/**
 * Analog of try with resources or [AutoCloseable.use] in Kotlin.
 * Takes a suspending [block] function and calls it on this[CloseableProcessor] instance.
 * Regardless whether exception is thrown or not calls [CloseableProcessor.close] method.
 */
suspend fun <T : CloseableProcessor, R> T.use(block: suspend (T) -> R): R {
    return try {
        block(this)
    } finally {
        this.close()
    }
}

