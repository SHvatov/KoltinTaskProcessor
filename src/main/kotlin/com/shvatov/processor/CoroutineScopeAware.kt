package com.shvatov.processor

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.slf4j.Logger

/**
 * Coroutine [scope], which is used to launch
 * @author shvatov
 */
interface CoroutineScopeAware {
    val scope: CoroutineScope

    companion object LoggingExceptionHandlerProvider {
        /**
         * Creates an instance of [CoroutineExceptionHandler] to be used in the
         * sub-coroutines for logging the exception. Uses [org.slf4j.Logger] instance.
         */
        fun exceptionHandler(log: Logger): CoroutineExceptionHandler {
            return CoroutineExceptionHandler { ctx, exception ->
                log.error(
                    "Exception occurred while processing data in ${ctx[CoroutineName]}:",
                    exception
                )
            }
        }
    }
}