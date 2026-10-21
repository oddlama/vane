package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

/** Base class for teleport scroll items with shared cooldown and inhibition logic. */
abstract class Scroll(context: Context<Trifles?>, private val defaultCooldown: Int) :
    CustomItem<Trifles?>(context) {
    /** Configurable scroll-use cooldown in milliseconds. */
    @JvmField
    @ConfigInt(
        def = 0,
        min = 0,
        desc = "Cooldown in milliseconds until another scroll can be used.",
    )
    var configCooldown: Int = 0

    /** Controls whether mending can repair this scroll type. */
    @ConfigBoolean(
        def = false,
        desc = "Allow this scroll to be repaired via the mending enchantment.",
    )
    private val configAllowMending = false

    /** Provides the default cooldown fallback used by config injection. */
    fun configCooldownDef(): Int = defaultCooldown

    /**
     * Get the teleport location for the given player. Return null to prevent teleporting. Cooldown
     * is already handled by the base class, you only need to assert that a valid location is
     * available. For example, home scrolls may prevent teleport because of a missing bed or respawn
     * point here and notify the player about that. If imminentTeleport is true, the player will be
     * teleported if this function returns a valid location. The player should only be notified of
     * errors if this is set.
     */
    abstract fun teleportLocation(
        scroll: ItemStack?,
        player: Player?,
        imminentTeleport: Boolean,
    ): Location?

    /** Returns the behavior inhibition set shared by all scroll variants. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> =
        EnumSet.of(
                InhibitBehavior.USE_IN_VANILLA_RECIPE,
                InhibitBehavior.TEMPT,
                InhibitBehavior.USE_OFFHAND,
            )
            .apply {
                if (!configAllowMending) {
                    add(InhibitBehavior.MEND)
                }
            }
}
