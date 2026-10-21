package org.oddlama.vane.core

import org.oddlama.vane.core.lang.LangField

/** Base exception for YAML loading/validation failures. */
open class YamlLoadException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** Language YAML exception variant that includes the associated language field. */
    class Lang(message: String?, val langField: LangField<*>) : YamlLoadException(message) {
        override val message: String
            get() = "[${langField}] ${super.message}"
    }
}
