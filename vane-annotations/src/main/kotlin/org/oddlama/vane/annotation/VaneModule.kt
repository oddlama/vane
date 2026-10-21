package org.oddlama.vane.annotation

/**
 * Marks a class as a Vane module and provides metadata used by the framework.
 *
 * @property name Human-readable module name.
 * @property bstats bStats ID for metrics reporting, or -1 if none.
 * @property configVersion Version number of the module configuration format.
 * @property langVersion Version number of the module language/messages format.
 * @property storageVersion Version number of the module storage format.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneModule(
    val name: String,
    val bstats: Int = -1,
    val configVersion: Long,
    val langVersion: Long,
    val storageVersion: Long,
)
