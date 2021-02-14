package com.shvatov.processor.data

import kotlinx.coroutines.CoroutineScope

/**
 * @author shvatov
 */
data class Task<P : Any, R : Any>(
    /**
     * Object, which is used as a argument for the provided [action].
     */
    val payload: P,

    /**
     * Task itself, which must be computed by the processor instance.
     * It is an extension on [CoroutineScope], so that it can be cancellable,
     * otherwise timeout will have no effect whatsoever.
     */
    val action: CoroutineScope.(P) -> R?
)