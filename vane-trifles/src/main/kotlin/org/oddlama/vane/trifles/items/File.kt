package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.type.*
import org.bukkit.block.data.type.Tripwire
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil

@VaneItem(
    name = "file",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 4000,
    modelData = 0x760003,
    version = 1,
)
/** Utility tool that cycles block-state variants for stairs, walls, and multi-facing blocks. */
class File(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    /** Defines the file crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" M", "S ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        )
    }

    /** Prevents conflicting vanilla interactions for the file item. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND,
        )
    }

    /** Returns the next allowed facing from a sorted list of valid facings. */
    private fun nextFacing(allowedFaces: MutableSet<BlockFace?>, face: BlockFace?): BlockFace? {
        if (allowedFaces.isEmpty()) {
            return face
        }
        val list = allowedFaces.filterNotNull().sortedBy { it.ordinal }
        if (list.isEmpty()) {
            return face
        }
        val index = list.indexOf(face)
        if (index == -1) {
            return face
        }
        return list[(index + 1) % list.size]
    }

    /** Toggles stair half between top and bottom. */
    private fun changeStairHalf(stairs: Stairs): Sound? {
        // Change half
        stairs.half =
            if (stairs.half == Bisected.Half.BOTTOM) Bisected.Half.TOP else Bisected.Half.BOTTOM
        return Sound.UI_STONECUTTER_TAKE_RESULT
    }

    /** Rotates a cardinal face counter-clockwise. */
    private fun nextFaceCcw(face: BlockFace): BlockFace {
        return when (face) {
            BlockFace.NORTH -> BlockFace.WEST
            BlockFace.EAST -> BlockFace.NORTH
            BlockFace.SOUTH -> BlockFace.EAST
            BlockFace.WEST -> BlockFace.SOUTH
            else -> face
        }
    }

    /** Cycles stair shape based on clicked octant and current stair orientation. */
    private fun changeStairShape(player: Player, block: Block?, stairs: Stairs): Sound? {
        // Check which eighth of the block was clicked
        val oct = BlockUtil.raytraceOct(player, block) ?: return null

        var corner = oct.corner()
        val isStairTop = stairs.half == Bisected.Half.TOP

        // If we clicked on the base part, toggle the corner upstate,
        // as we always want to reason abount the corner to add/remove.
        if (corner.up() == isStairTop) {
            corner = corner.up(!corner.up())
        }

        // Rotate a corner so that we can interpret it from
        // the reference facing (north)
        val originalFacing = stairs.facing
        corner = corner.rotateToNorthReference(originalFacing)

        // Determine resulting shape/facing and whether geometry is added or removed.
        var shape: Stairs.Shape? = null
        var face: BlockFace? = null
        var added = false
        when (stairs.shape) {
            Stairs.Shape.STRAIGHT ->
                when (corner.xzFace()) {
                    BlockFace.SOUTH_WEST -> {
                        shape = Stairs.Shape.INNER_LEFT
                        face = BlockFace.NORTH
                        added = true
                    }

                    BlockFace.NORTH_WEST -> {
                        shape = Stairs.Shape.OUTER_RIGHT
                        face = BlockFace.NORTH
                        added = false
                    }

                    BlockFace.NORTH_EAST -> {
                        shape = Stairs.Shape.OUTER_LEFT
                        face = BlockFace.NORTH
                        added = false
                    }

                    BlockFace.SOUTH_EAST -> {
                        shape = Stairs.Shape.INNER_RIGHT
                        face = BlockFace.NORTH
                        added = true
                    }

                    else -> {}
                }

            Stairs.Shape.INNER_LEFT ->
                when (corner.xzFace()) {
                    BlockFace.SOUTH_WEST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.NORTH
                        added = false
                    }

                    BlockFace.NORTH_EAST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.WEST
                        added = false
                    }

                    else -> {}
                }

            Stairs.Shape.INNER_RIGHT ->
                when (corner.xzFace()) {
                    BlockFace.NORTH_WEST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.EAST
                        added = false
                    }

                    BlockFace.SOUTH_EAST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.NORTH
                        added = false
                    }

                    else -> {}
                }

            Stairs.Shape.OUTER_LEFT ->
                when (corner.xzFace()) {
                    BlockFace.SOUTH_WEST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.WEST
                        added = true
                    }

                    BlockFace.NORTH_EAST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.NORTH
                        added = true
                    }

                    else -> {}
                }

            Stairs.Shape.OUTER_RIGHT ->
                when (corner.xzFace()) {
                    BlockFace.NORTH_WEST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.NORTH
                        added = true
                    }

                    BlockFace.SOUTH_EAST -> {
                        shape = Stairs.Shape.STRAIGHT
                        face = BlockFace.EAST
                        added = true
                    }

                    else -> {}
                }
        }

        // Break if the resulting shape is invalid
        if (shape == null) {
            return null
        }

        // Undo reference rotation
        when (face) {
            BlockFace.NORTH -> face = originalFacing
            BlockFace.EAST -> face = nextFaceCcw(originalFacing).oppositeFace
            BlockFace.WEST -> face = nextFaceCcw(originalFacing)
            else -> {}
        }

        stairs.shape = shape
        stairs.facing = face ?: return null

        return if (added) Sound.UI_STONECUTTER_TAKE_RESULT else Sound.BLOCK_GRINDSTONE_USE
    }

    /** Toggles side connections on multiple-facing blocks and returns a feedback sound. */
    private fun changeMultipleFacing(
        player: Player,
        block: Block?,
        mf: MultipleFacing?,
        clickedFace: BlockFace,
    ): Sound? {
        var clickedFace = clickedFace
        if (mf is Fence || mf is GlassPane) {
            // Trace which side is the dominant side
            val result = BlockUtil.raytraceDominantFace(player, block) ?: return null

            // Only replace facing choice if we did hit a side,
            // or if the dominance was big enough.
            if (
                result.dominance > .2 ||
                    (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)
            ) {
                clickedFace = result.face ?: return null
            }
        } else if (mf !is Tripwire) {
            return null
        }

        // Check if the clicked face is allowed to change
        if (!mf.allowedFaces.contains(clickedFace)) {
            return null
        }

        val hasFace = mf.hasFace(clickedFace)
        if (hasFace && mf.faces.isEmpty()) {
            // Refuse to remove beyond minimum face count
            return null
        }

        // Toggle clicked block face
        mf.setFace(clickedFace, !hasFace)

        // Choose sound
        return if (hasFace) Sound.BLOCK_GRINDSTONE_USE else Sound.UI_STONECUTTER_TAKE_RESULT
    }

    /** Adjusts wall post/side heights based on click position and dominant hit face. */
    private fun changeWall(
        player: Player,
        block: Block?,
        wall: Wall,
        clickedFace: BlockFace?,
    ): Sound? {
        // Trace which side is the dominant side
        val result = BlockUtil.raytraceDominantFace(player, block) ?: return null
        // Only replace facing choice if we did hit a side,
        // or if the dominance was big enough.
        val adjustedClickedFace =
            if (
                result.dominance > .2 ||
                    (clickedFace != BlockFace.UP && clickedFace != BlockFace.DOWN)
            ) {
                result.face
            } else {
                clickedFace
            }

        if (clickedFace == BlockFace.UP) {
            val wasUp = wall.isUp
            if (adjustedClickedFace == BlockFace.UP) {
                // click top in middle -> toggle up
                wall.isUp = !wasUp
            } else if (BlockUtil.XZ_FACES.contains(adjustedClickedFace)) {
                // click top on side -> toggle height
                val sideFace = adjustedClickedFace ?: return null
                val height = wall.getHeight(sideFace)
                when (height) {
                    Wall.Height.NONE -> return null
                    Wall.Height.LOW -> wall.setHeight(sideFace, Wall.Height.TALL)
                    Wall.Height.TALL -> wall.setHeight(sideFace, Wall.Height.LOW)
                }
            }

            return if (wasUp) Sound.BLOCK_GRINDSTONE_USE else Sound.UI_STONECUTTER_TAKE_RESULT
        } else {
            // click side -> toggle side
            val sideFace = adjustedClickedFace ?: return null
            val hasFace = wall.getHeight(sideFace) != Wall.Height.NONE

            // Set height and choose sound
            if (hasFace) {
                wall.setHeight(sideFace, Wall.Height.NONE)
                return Sound.BLOCK_GRINDSTONE_USE
            } else {
                // Use opposite face's height, or low if there is nothing.
                var targetHeight = wall.getHeight(sideFace.oppositeFace)
                if (targetHeight == Wall.Height.NONE) {
                    targetHeight = Wall.Height.LOW
                }
                wall.setHeight(sideFace, targetHeight)
                return Sound.UI_STONECUTTER_TAKE_RESULT
            }
        }
    }

    /** Handles right-click block editing with the file tool. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Require this custom file item.
        val player = event.player
        val hand = event.hand ?: return
        val item = player.equipment.getItem(hand)
        if (!isInstance(item)) {
            return
        }

        // Respect protections by simulating a block-break permission check.
        val block = event.clickedBlock ?: return
        val breakEvent = BlockBreakEvent(block, player)
        val server = module?.server ?: return
        server.pluginManager.callEvent(breakEvent)
        if (breakEvent.isCancelled) {
            return
        }

        val data = block.blockData
        val clickedFace = event.blockFace

        val sound: Sound?
        if (player.isSneaking) {
            if (data is Stairs) {
                sound = changeStairHalf(data)
            } else {
                return
            }
        } else {
            sound =
                when (data) {
                    is MultipleFacing -> {
                        changeMultipleFacing(player, block, data, clickedFace)
                    }

                    is Wall -> {
                        changeWall(player, block, data, clickedFace)
                    }

                    is Stairs -> {
                        changeStairShape(player, block, data)
                    }

                    else -> {
                        return
                    }
                }
        }

        // Abort if no state transition was applied.
        if (sound == null) {
            return
        }

        // Apply without physics updates to avoid cascading neighbor state changes.
        block.setBlockData(data, false)
        block.world.playSound(block.location, sound, SoundCategory.BLOCKS, 1.0f, 1.0f)

        // Consume durability and play swing animation.
        ItemUtil.damageItem(player, item, 1)
        PlayerUtil.swingArm(player, hand)
    }
}
