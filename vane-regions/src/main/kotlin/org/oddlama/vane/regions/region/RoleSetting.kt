package org.oddlama.vane.regions.region

import org.oddlama.vane.regions.Regions

/** Permission switches available on region roles. */
enum class RoleSetting(private val def: Boolean, private val defAdmin: Boolean) {
    ADMIN(false, true),
    BUILD(false, true),
    USE(true, true),
    CONTAINER(false, true),
    PORTAL(false, true);

    /** Returns the default value for normal users or admins. */
    fun defaultValue(admin: Boolean): Boolean = if (admin) defAdmin else def

    /** Returns whether a global override currently applies to this role setting. */
    fun hasOverride(): Boolean = override != 0

    /** Global override value from `Regions.roleOverrides`. */
    val override: Int
        get() = Regions.roleOverrides?.getOverride(this) ?: 0
}
