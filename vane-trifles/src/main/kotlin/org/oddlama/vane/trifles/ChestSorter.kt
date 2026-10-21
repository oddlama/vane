package org.oddlama.vane.trifles

import java.util.*
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.Barrel
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.data.Directional
import org.bukkit.block.data.FaceAttachable
import org.bukkit.block.data.FaceAttachable.AttachedFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.data.CooldownData
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil

/** Sorts nearby chest and barrel inventories when a configured button is pressed. */
class ChestSorter(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group("ChestSorting", "Enables chest sorting when a nearby button is pressed.")
    ) {
    /** Cooldown in milliseconds before a container can be sorted again. */
    @ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
    var configCooldown: Long = 0

    /** Persistent cooldown helper keyed by container block state. */
    private var cooldownData: CooldownData? = null

    /** Search radius on the local X axis relative to the activated button. */
    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc =
            "Chest sorting radius in X-direction from the button (left-right when looking at the button). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned.",
    )
    /** Search radius on the local X axis relative to the activated button. */
    var configRadiusX: Int = 0

    /** Search radius on the local Y axis relative to the activated button. */
    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc =
            "Chest sorting radius in Y-direction from the button (up-down when looking at the button - this can be a horizontal direction if the button is on the ground). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned.",
    )
    /** Search radius on the local Y axis relative to the activated button. */
    var configRadiusY: Int = 0

    /** Search radius on the local Z axis relative to the activated button. */
    @ConfigInt(
        def = 1,
        min = 0,
        max = 16,
        desc =
            "Chest sorting radius in Z-direction from the button (into/out-of the attached block). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned.",
    )
    /** Search radius on the local Z axis relative to the activated button. */
    var configRadiusZ: Int = 0

    /** Rebuilds cooldown state whenever relevant configuration changes. */
    override fun onConfigChange() {
        super.onConfigChange()
        this.cooldownData = CooldownData(LAST_SORT_TIME, configCooldown)
    }

    /** Condenses, restacks, and then sorts a single inventory by item comparator. */
    private fun sortInventory(inventory: Inventory) {
        // Count non-null stacks to size the condensed list once.
        val savedContents = inventory.storageContents
        var nonNull = 0
        for (i in savedContents) {
            if (i != null) {
                ++nonNull
            }
        }

        // Clone all non-null stacks into a temporary compact list.
        val savedContentsCondensedList: MutableList<ItemStack> = ArrayList(nonNull)
        for (i in savedContents) {
            if (i != null) {
                savedContentsCondensedList.add(i.clone())
            }
        }

        // Reinsert items so Bukkit merges stacks; restore original contents on any failure.
        try {
            inventory.clear()
            val leftovers = inventory.addItem(*savedContentsCondensedList.toTypedArray())
            if (leftovers.isNotEmpty()) {
                // Restack failed unexpectedly, so roll back to preserve player items.
                inventory.storageContents = savedContents
                module?.log?.warning("Sorting inventory $inventory produced leftovers!")
            }
        } catch (e: Exception) {
            inventory.storageContents = savedContents
            throw e
        }

        // Sort the merged contents for a stable storage layout.
        val contents = inventory.storageContents
        Arrays.sort(contents, ItemUtil.ItemStackComparator())
        inventory.storageContents = contents
    }

    /** Sorts a generic container while respecting its cooldown entry. */
    private fun sortContainer(container: Container) {
        // Skip sorting if the container is still in cooldown.
        val cooldownData = cooldownData ?: return
        if (!cooldownData.checkOrUpdateCooldown(container)) {
            return
        }

        sortInventory(container.inventory)
    }

    /** Sorts chest inventories, including double chests via a deterministic side. */
    private fun sortChest(chest: Chest) {
        val inventory = chest.inventory

        // Resolve the side used to store persistent cooldown data.
        val persistentChest: Chest
        if (inventory is DoubleChestInventory) {
            val leftSide = (inventory.leftSide).holder
            if (leftSide !is Chest) {
                return
            }
            persistentChest = leftSide
        } else {
            persistentChest = chest
        }

        // Skip sorting if the resolved chest side is still in cooldown.
        val cooldownData = cooldownData ?: return
        if (!cooldownData.checkOrUpdateCooldown(persistentChest)) {
            return
        }

        if (persistentChest !== chest) {
            // Persist through the left side when this event references the right side.
            persistentChest.update(true, false)
        }

        sortInventory(inventory)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = false,
    ) // Keep disabled-cancel filtering to observe all block-use outcomes.
    /** Handles button interaction and sorts nearby storage containers in configured bounds. */
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Require vanilla interaction to be allowed for this block.
        if (event.useInteractedBlock() != Event.Result.ALLOW) {
            return
        }

        // Only trigger from button blocks.
        val rootBlock = event.clickedBlock ?: return
        if (!Tag.BUTTONS.isTagged(rootBlock.type)) {
            return
        }

        val buttonData = rootBlock.state.blockData
        val facing = (buttonData as Directional).facing
        val face = (buttonData as FaceAttachable).attachedFace

        var rx = 0
        val ry: Int
        var rz = 0

        // Convert configured radii into world-relative axes based on button mounting.
        if (face == AttachedFace.WALL) {
            ry = configRadiusY
            when (facing) {
                BlockFace.NORTH,
                BlockFace.SOUTH -> {
                    rx = configRadiusX
                    rz = configRadiusZ
                }

                BlockFace.EAST,
                BlockFace.WEST -> {
                    rx = configRadiusZ
                    rz = configRadiusX
                }

                else -> {}
            }
        } else {
            ry = configRadiusZ
            when (facing) {
                BlockFace.NORTH,
                BlockFace.SOUTH -> {
                    rx = configRadiusX
                    rz = configRadiusY
                }

                BlockFace.EAST,
                BlockFace.WEST -> {
                    rx = configRadiusY
                    rz = configRadiusX
                }

                else -> {}
            }
        }

        // Iterate configured area and sort all supported container types.
        for (x in -rx..rx) {
            for (y in -ry..ry) {
                for (z in -rz..rz) {
                    val block = rootBlock.getRelative(x, y, z)
                    val state = block.state
                    if (state is Chest) {
                        sortChest(state)
                    } else if (state is Barrel) {
                        sortContainer(state)
                    }
                }
            }
        }
    }

    companion object {
        /** Key storing the timestamp of the last successful sort for a container. */
        val LAST_SORT_TIME: NamespacedKey =
            StorageUtil.namespacedKey("vane_trifles", "last_sort_time")
    }
}
