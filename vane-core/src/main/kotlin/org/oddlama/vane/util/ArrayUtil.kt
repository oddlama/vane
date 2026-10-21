package org.oddlama.vane.util

/** Returns a copy of this array with [element] inserted at the beginning. */
fun <T> Array<T?>.prepend(element: T?): Array<T?> =
    copyOf(size + 1).also {
        System.arraycopy(this, 0, it, 1, size)
        it[0] = element
    }

/** Returns a copy of this array with [element] appended at the end. */
fun <T> Array<T?>.append(element: T?): Array<T?> = copyOf(size + 1).also { it[size] = element }
