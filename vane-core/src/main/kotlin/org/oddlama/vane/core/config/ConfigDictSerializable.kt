package org.oddlama.vane.core.config

/** Contract for dictionary-like config objects used by [ConfigDictField]. */
interface ConfigDictSerializable {
    /** Serializes this object to a mutable dictionary. */
    fun toDict(): MutableMap<String, Any>

    /** Populates this object from a mutable dictionary. */
    fun fromDict(dict: MutableMap<String, Any>)
}
