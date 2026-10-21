package org.oddlama.vane.trifles

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/** Finds nearby containers/entities that contain a requested material and highlights matches. */
class ItemFinder(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "ItemFinder",
            "Enables players to search for items in nearby containers by either shift-right-clicking a similar item in their inventory or by using the `/finditem <item>` command.",
        )
    ) {
    /** Strong reference to the owning module used for registration and scheduling. */
    private val moduleRef = requireNotNull(module)

    /** Radius in chunks around the player to scan for matching inventories. */
    @ConfigInt(
        def = 2,
        min = 1,
        max = 10,
        desc =
            "The radius of chunks in which containers (and possibly entities) are checked for matching items.",
    )
    /** Radius in chunks around the player to scan for matching inventories. */
    var configRadius: Int = 0

    /** Whether inventory-bearing entities are included in the search. */
    @ConfigBoolean(def = true, desc = "Also search entities such as players, mobs, minecarts, ...")
    var configSearchEntities: Boolean = false

    /** Whether shift-right-click shortcut usage requires an explicit permission. */
    @ConfigBoolean(
        def = false,
        desc =
            "Only allow players to use the shift+rightclick shortcut when they have the shortcut permission `vane.trifles.use_item_find_shortcut`.",
    )
    /** Whether shift-right-click shortcut usage requires an explicit permission. */
    var configRequirePermission: Boolean = false

    /** Permission checked when shortcut usage is configured to require it. */
    val useItemFindShortcutPermission: Permission =
        Permission(
            "vane.${moduleRef.annotationName}.use_item_find_shortcut",
            "Allows a player to use shfit+rightclick to search for items if the require_permission config is set",
            PermissionDefault.FALSE,
        )

    /** Registers the shortcut permission in Bukkit's permission manager. */
    init {
        moduleRef.registerPermission(useItemFindShortcutPermission)
    }

    /** Handles inventory shortcut activation to start an item search. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerClickInventory(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }

        val player = event.whoClicked as? Player ?: return

        if (configRequirePermission && !player.hasPermission(useItemFindShortcutPermission)) {
            return
        }

        val item = event.currentItem
        if (item == null || item.type == Material.AIR) {
            return
        }

        // Trigger only for shift-right-click quick moves.
        if (
            !(event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                event.click == ClickType.SHIFT_RIGHT)
        ) {
            return
        }

        event.isCancelled = true
        if (findItem(player, item.type)) {
            scheduleNextTick { player.closeInventory() }
        }
    }

    /** Returns whether a block state is a searchable container. */
    private fun isContainer(block: Block): Boolean {
        return block.state is Container
    }

    /** Emits particle-based fallback highlighting when packet integration is unavailable. */
    private fun fallbackIndicateMatch(player: Player, location: Location) {
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.4, 0.0, 0.0, 0.0)
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.4, 0.0, 0.0)
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.0, 0.4, 0.0)
        player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location, 70, 0.2, 0.2, 0.2, 0.0)
    }

    /**
     * Searches configured nearby chunks for the given material.
     *
     * @return `true` when at least one matching inventory was found.
     */
    fun findItem(player: Player, material: Material): Boolean {
        val module = module ?: return false

        // Scan loaded chunks in range and highlight every matching inventory source.
        var anyFound = false
        val world = player.world
        val originChunk = player.chunk
        val packetEventsEnabled = module.packetEventsEnabled
        for (cx in originChunk.x - configRadius..originChunk.x + configRadius) {
            for (cz in originChunk.z - configRadius..originChunk.z + configRadius) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue
                }
                val chunk = world.getChunkAt(cx, cz)
                for (tileEntity in chunk.getTileEntities({ block -> isContainer(block) }, false)) {
                    if (tileEntity is Container && tileEntity.inventory.contains(material)) {
                        if (tileEntity.type == Material.CHEST) {
                            if (packetEventsEnabled) {
                                ItemFinderPacketUtils.indicateChestMatch(module, player, tileEntity)
                            } else {
                                fallbackIndicateMatch(
                                    player,
                                    tileEntity.location.add(0.5, 0.5, 0.5),
                                )
                            }
                        } else {
                            if (packetEventsEnabled) {
                                ItemFinderPacketUtils.indicateContainerMatch(
                                    module,
                                    player,
                                    tileEntity,
                                )
                            } else {
                                fallbackIndicateMatch(
                                    player,
                                    tileEntity.location.add(0.5, 0.5, 0.5),
                                )
                            }
                        }
                        anyFound = true
                    }
                }
                if (configSearchEntities) {
                    for (entity in chunk.entities) {
                        // Don't indicate the player
                        if (entity === player) {
                            continue
                        }

                        if (entity is InventoryHolder) {
                            if (entity.inventory.contains(material)) {
                                if (packetEventsEnabled) {
                                    ItemFinderPacketUtils.indicateEntityMatch(
                                        module,
                                        player,
                                        entity,
                                    )
                                } else {
                                    fallbackIndicateMatch(player, entity.location)
                                }
                                anyFound = true
                            }
                        }
                    }
                }
            }
        }

        if (anyFound) {
            player.playSound(
                player,
                Sound.BLOCK_AMETHYST_BLOCK_HIT,
                SoundCategory.MASTER,
                1.0f,
                1.3f,
            )
        } else {
            player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f)
        }

        return anyFound
    }
}
