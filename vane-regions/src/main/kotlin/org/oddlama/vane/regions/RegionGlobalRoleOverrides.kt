package org.oddlama.vane.regions

import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.RoleSetting

/**
 * Global role-setting overrides applied across every region role on the server.
 */
class RegionGlobalRoleOverrides(context: Context<Regions?>) : ModuleComponent<Regions?>(
    context.namespace(
        "GlobalRoleOverrides",
        "This controls global role setting overrides for all roles in every region on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all roles in every region, `-1` force-disables respectively. Force-disable naturally also affects the owner, so be careful!"
    )
) {
    /**
     * Override value for the `ADMIN` role setting.
     */
    @ConfigInt(
        def = 0,
        min = -1,
        max = 1,
        desc = "Overrides the admin permission. Be careful, this is almost never what you want and may result in immutable regions."
    )
    var configAdmin: Int = 0

    /**
     * Override value for the `BUILD` role setting.
     */
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the build permission.")
    var configBuild: Int = 0

    /**
     * Override value for the `USE` role setting.
     */
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the use permission.")
    var configUse: Int = 0

    /**
     * Override value for the `CONTAINER` role setting.
     */
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the container permission.")
    var configContainer: Int = 0

    /**
     * Override value for the `PORTAL` role setting.
     */
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the portal permission.")
    var configPortal: Int = 0

    /**
     * Returns the configured global override for the given role setting.
     */
    fun getOverride(setting: RoleSetting): Int = when (setting) {
        RoleSetting.ADMIN -> configAdmin
        RoleSetting.BUILD -> configBuild
        RoleSetting.USE -> configUse
        RoleSetting.CONTAINER -> configContainer
        RoleSetting.PORTAL -> configPortal
    }

    /**
     * No-op lifecycle hook.
     */
    override fun onEnable() {}

    /**
     * No-op lifecycle hook.
     */
    override fun onDisable() {}
}
