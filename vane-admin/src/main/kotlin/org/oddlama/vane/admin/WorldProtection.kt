package org.oddlama.vane.admin

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/** Prevents world modifications for players without the bypass permission. */
class WorldProtection(context: Context<Admin?>) :
    Listener<Admin?>(
        context.group(
            "WorldProtection",
            "Enable world protection. This will prevent anyone from modifying the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'.",
            false,
        )
    ) {
    private val admin: Admin
        get() = requireNotNull(module)

    private val permission =
        Permission(
            PERMISSION_NAME,
            "Allow player to modify world",
            PermissionDefault.OP,
        )

    init {
        admin.registerPermission(permission)
    }

    /** Returns true when the given entity is a player without world-modification permission. */
    fun denyModifyWorld(entity: Entity?): Boolean =
        (entity as? Player)?.hasPermission(permission) == false

    /** Handles block break attempts in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles block placement attempts in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles enchant preparation in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareItemEnchant(event: PrepareItemEnchantEvent) {
        if (denyModifyWorld(event.enchanter)) {
            event.isCancelled = true
        }
    }

    /** Handles enchant applications in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onItemEnchant(event: EnchantItemEvent) {
        if (denyModifyWorld(event.enchanter)) {
            event.isCancelled = true
        }
    }

    /** Handles entity-caused combustion in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityCombustByEntity(event: EntityCombustByEntityEvent) {
        if (denyModifyWorld(event.combuster)) {
            event.isCancelled = true
        }
    }

    /** Handles entity damage interactions in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val denied =
            when (event) {
                is EntityDamageByEntityEvent ->
                    denyModifyWorld(event.damager) || denyModifyWorld(event.entity)
                else -> denyModifyWorld(event.entity)
            }

        if (denied) {
            event.isCancelled = true
        }
    }

    /** Handles hunger changes in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityFoodLevelChange(event: FoodLevelChangeEvent) {
        if (denyModifyWorld(event.entity)) {
            event.isCancelled = true
        }
    }

    /** Handles hanging-entity break actions in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        if (denyModifyWorld(event.remover)) {
            event.isCancelled = true
        }
    }

    /** Handles hanging-entity placement in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles crafting interaction in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onItemCraft(event: CraftItemEvent) {
        if (denyModifyWorld(event.whoClicked)) {
            event.isCancelled = true
        }
    }

    /** Handles player armor-stand manipulation in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles player bucket empty actions in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles player bucket fill actions in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles player book edits in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerEditBook(event: PlayerEditBookEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles entity interaction initiated by players in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles generic player interaction in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    /** Handles shearing actions in protected worlds. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        if (denyModifyWorld(event.player)) {
            event.isCancelled = true
        }
    }

    companion object {
        private const val PERMISSION_NAME = "vane.admin.modify_world"
    }
}
