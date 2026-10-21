package org.oddlama.vane.core.functional

/** Erased six-argument consumer functor. */
fun interface Consumer6<T1, T2, T3, T4, T5, T6> : ErasedFunctor, GenericsFinder {
    /** Applies this consumer. */
    fun apply(t1: T1?, t2: T2?, t3: T3?, t4: T4?, t5: T5?, t6: T6?)

    /** Invokes this consumer with erased arguments. */
    @Suppress("UNCHECKED_CAST")
    override fun invoke(args: List<Any?>): Any? {
        require(args.size == 6) { "Functor needs 6 arguments but got ${args.size} arguments" }
        apply(
            args[0] as T1?,
            args[1] as T2?,
            args[2] as T3?,
            args[3] as T4?,
            args[4] as T5?,
            args[5] as T6?,
        )
        return null
    }
}
