package org.oddlama.vane.util

import java.lang.reflect.Field

/** Reflection helpers for the annotation-driven managers in vane-core. */
object ReflectionUtil {
    /**
     * Returns every field declared by [cls] and by each of its superclasses, most-derived first.
     *
     * This replaces `org.reflections.ReflectionUtils.getAllFields`, which was the only thing the
     * whole `org.reflections` library (~300 KiB) was used for. Interfaces are deliberately not
     * walked: their fields are implicitly `static final` constants and can never carry the
     * instance-field annotations the config, lang and persistent managers look for. Comparing the
     * two implementations over every class in the project bears that out — they differ only on the
     * `Companion` constant of an interface, and on no annotated field anywhere.
     *
     * A field shadowed by a subclass is returned once per declaring class, matching the previous
     * behaviour — the two `Field` objects are distinct, so the old `Set` did not collapse them.
     *
     * @param cls Class to collect fields from.
     * @return All declared fields along the superclass chain.
     */
    @JvmStatic
    fun allFields(cls: Class<*>): List<Field> =
        generateSequence(cls) { it.superclass }.flatMap { it.declaredFields.asSequence() }.toList()
}
