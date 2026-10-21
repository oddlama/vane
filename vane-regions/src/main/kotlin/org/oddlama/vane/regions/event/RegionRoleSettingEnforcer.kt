package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.RoleSetting

/** Enforces role-based interaction permissions inside claimed regions. */
class RegionRoleSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    /** Owning regions module instance. */
    private val regions: Regions
        get() = requireNotNull(module)

    /** Checks whether a role setting at a location matches the expected value. */
    fun checkSettingAt(
        location: Location,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean,
    ): Boolean {
        /** Region at the queried location. */
        val region = regions.regionAt(location) ?: return false

        /** Region group owning that region. */
        val group = region.regionGroup(regions) ?: return false

        /** Effective player role inside the region group. */
        val role = group.getRole(player.uniqueId) ?: return false
        return role.getSetting(setting) == checkAgainst
    }

    /** Checks whether a role setting at a block matches the expected value. */
    fun checkSettingAt(
        block: Block,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean,
    ): Boolean {
        /** Region containing the queried block. */
        val region = regions.regionAt(block) ?: return false

        /** Region group owning that region. */
        val group = region.regionGroup(regions) ?: return false

        /** Effective player role inside the region group. */
        val role = group.getRole(player.uniqueId) ?: return false
        return role.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels block breaking when `BUILD` is denied. */
    fun onBlockBreak(event: BlockBreakEvent) {
        // Prevent breaking of region blocks
        if (checkSettingAt(event.block, event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels block placement when `BUILD` is denied. */
    fun onBlockPlace(event: BlockPlaceEvent) {
        // Prevent (re-)placing of region blocks
        if (checkSettingAt(event.block, event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts armor-stand and item-frame manipulation by role settings. */
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damaged = event.entity
        val damager = event.damager

        when (damaged.type) {
            EntityType.ARMOR_STAND -> {
                if (damager !is Player) {
                    return
                }

                if (checkSettingAt(damaged.location.block, damager, RoleSetting.BUILD, false)) {
                    event.isCancelled = true
                }
                return
            }

            EntityType.ITEM_FRAME -> {
                if (damager !is Player) {
                    return
                }

                val itemFrame = damaged as ItemFrame
                val item = itemFrame.item
                if (item.type != Material.AIR) {
                    // This is a player taking the item out of an item-frame
                    if (
                        checkSettingAt(
                            damaged.location.block,
                            damager,
                            RoleSetting.CONTAINER,
                            false,
                        )
                    ) {
                        event.isCancelled = true
                    }
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels hanging-entity removal when `BUILD` is denied. */
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        var player: Player? = null

        if (remover is Player) {
            player = remover
        } else if (remover is Projectile) {
            val shooter = remover.shooter
            if (shooter is Player) {
                player = shooter
            }
        }

        if (
            player != null &&
                checkSettingAt(event.entity.location, player, RoleSetting.BUILD, false)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels hanging placement when `BUILD` is denied. */
    fun onHangingPlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        if (checkSettingAt(event.entity.location, player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels armor-stand interactions when `BUILD` is denied. */
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (checkSettingAt(event.rightClicked.location, event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels bucket-empty actions when `BUILD` is denied. */
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (checkSettingAt(event.blockClicked, event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels bucket-fill actions when `BUILD` is denied. */
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (checkSettingAt(event.blockClicked, event.player, RoleSetting.BUILD, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels item-frame interaction when `CONTAINER` is denied. */
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity.type != EntityType.ITEM_FRAME) {
            return
        }

        // Place or rotate item
        if (checkSettingAt(entity.location, event.player, RoleSetting.CONTAINER, false)) {
            event.isCancelled = true
        }
    }

    // The EventPriority is HIGH, so this is executed AFTER the portals try
    // to activate, which is a seperate permission.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    /** Restricts pressure-plate, tripwire, and right-click use when `USE` is denied. */
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return

        when (event.action) {
            Action.PHYSICAL -> {
                if (Tag.PRESSURE_PLATES.isTagged(block.type)) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.isCancelled = true
                    }
                } else if (block.type == Material.TRIPWIRE) {
                    if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                        event.isCancelled = true
                    }
                }
                return
            }

            Action.RIGHT_CLICK_BLOCK -> {
                if (checkSettingAt(block, player, RoleSetting.USE, false)) {
                    event.isCancelled = true
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels container opening when `CONTAINER` is denied. */
    fun onPlayerInventoryOpen(event: InventoryOpenEvent) {
        // Only relevant if viewing should be prohibited, too.
        if (!regions.configProhibitViewingContainers) {
            return
        }

        if (event.player !is Player) {
            return
        }

        val player = event.player as Player
        if (checkContainerSettingAt(event.inventory, player)) {
            event.isCancelled = true
        }
    }

    /** Returns whether container access should be denied for this inventory and player. */
    private fun checkContainerSettingAt(
        inventory: org.bukkit.inventory.Inventory,
        player: Player,
    ): Boolean {
        val location = inventory.location ?: return false
        val holder = inventory.holder ?: return false
        return !(holder !is DoubleChest && holder !is Container && holder !is Minecart) &&
            checkSettingAt(
                location,
                player,
                RoleSetting.CONTAINER,
                false,
            )
        // Inventory is virtual / transient
    }

    /** Shared inventory-interaction enforcement for click and drag events. */
    fun onPlayerInventoryInteract(event: InventoryInteractEvent) {
        val clicker = event.whoClicked
        if (clicker !is Player) {
            return
        }

        if (checkContainerSettingAt(event.inventory, clicker)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Delegates inventory click checks to shared container enforcement. */
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        onPlayerInventoryInteract(event)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Delegates inventory drag checks to shared container enforcement. */
    fun onPlayerInventoryDrag(event: InventoryDragEvent) {
        onPlayerInventoryInteract(event)
    }
}
