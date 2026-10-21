package org.oddlama.vane.core.functional

/** Common erased invocation contract for functional wrappers. */
interface ErasedFunctor {
    /** Invokes this functor with erased argument list. */
    fun invoke(args: List<Any?>): Any?
}
