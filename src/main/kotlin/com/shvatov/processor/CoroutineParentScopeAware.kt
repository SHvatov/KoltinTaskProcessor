package com.shvatov.processor

import kotlinx.coroutines.CoroutineScope

/**
 * Contains a reference to the [parentScope] of the coroutine. Can be used for
 * structured concurrency, by making sub-coroutines extend this scope.
 *
 * Note: This interface and [CoroutineScopeAware] one are used to implement
 * structured concurrency using standard [CoroutineScope]. Each `processor`
 * is aware of its scope and must implement [CoroutineScopeAware]. Also
 * it can be aware of the coroutine scope of the parent coroutine if such
 * is present.
 *
 * @author shvatov
 */
interface CoroutineParentScopeAware {
    val parentScope: CoroutineScope?
}