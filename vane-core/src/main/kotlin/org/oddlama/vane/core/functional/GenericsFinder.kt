package org.oddlama.vane.core.functional

import java.io.Serializable
import java.lang.invoke.SerializedLambda
import java.lang.reflect.Method

/** Utility interface for resolving backing method metadata of Kotlin lambdas. */
interface GenericsFinder : Serializable {
    /** Resolves this lambda to a serialized lambda payload. */
    fun serialized(): SerializedLambda =
        runCatching {
                javaClass
                    .getDeclaredMethod("writeReplace")
                    .also { it.isAccessible = true }
                    .invoke(this) as SerializedLambda
            }
            .getOrElse { throw RuntimeException(it) }

    /** Resolves the containing implementation class of this lambda. */
    val containingClass: Class<*>
        get() =
            runCatching {
                    Class.forName(serialized().implClass.replace("/", "."))
                }
                .getOrElse { throw RuntimeException(it) }

    /** Resolves the implementation method backing this lambda. */
    fun method(): Method {
        val lambda = serialized()
        return containingClass.declaredMethods.firstOrNull { it.name == lambda.implMethodName }
            ?: throw RuntimeException("Method '${lambda.implMethodName}' not found")
    }
}
