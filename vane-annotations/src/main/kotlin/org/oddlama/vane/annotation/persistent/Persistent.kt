package org.oddlama.vane.annotation.persistent

/**
 * Marks a field as persistent so it will be stored and restored by the module's persistence/storage
 * system.
 */
@Retention(AnnotationRetention.RUNTIME) @Target(AnnotationTarget.FIELD) annotation class Persistent
