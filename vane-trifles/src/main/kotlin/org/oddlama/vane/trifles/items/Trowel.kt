package org.oddlama.vane.trifles.items

import java.util.*
import net.kyori.adventure.text.Component
import net.minecraft.advancements.triggers.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.lang.TranslatedMessageArray
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.Nms
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil

@VaneItem(
    name = "trowel",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 800,
    modelData = 0x76000e,
    version = 1,
)
/**
 * Places random block items from a selected inventory feed range.
 *
 * @param context module context used for registration.
 */
class Trowel(context: Context<Trifles?>) :
    org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    /**
     * Selectable inventory ranges used as block feed sources.
     *
     * @property displayName user-facing label for the feed source.
     * @property slots inventory slot indices included in this source.
     */
    enum class FeedSource(val displayName: String, val slots: IntArray) {
        HOTBAR("Hotbar", intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)),
        FIRST_ROW("First Inventory Row", intArrayOf(9, 10, 11, 12, 13, 14, 15, 16, 17)),
        SECOND_ROW("Second Inventory Row", intArrayOf(18, 19, 20, 21, 22, 23, 24, 25, 26)),
        THIRD_ROW("Third Inventory Row", intArrayOf(27, 28, 29, 30, 31, 32, 33, 34, 35));

        /** Returns the next feed source in cyclic order. */
        fun next(): FeedSource {
            return entries[(this.ordinal + 1) % entries.size]
        }

        /** Returns the user-facing feed source name. */
        override fun toString(): String {
            return displayName
        }
    }

    /** Lore template displayed with the currently selected feed source. */
    @LangMessageArray var langLore: TranslatedMessageArray? = null

    /** Defines the trowel crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("  S", "MM ")
                .setIngredient('M', Material.IRON_INGOT)
                .setIngredient('S', Material.STICK)
                .result(key().toString())
        )
    }

    /** Reads persisted feed source selection from item metadata. */
    private fun feedSource(itemStack: ItemStack): FeedSource {
        val meta = itemStack.itemMeta ?: return FeedSource.HOTBAR
        val ord =
            meta.persistentDataContainer.getOrDefault(FEED_SOURCE, PersistentDataType.INTEGER, 0)
        if (ord < 0 || ord >= FeedSource.entries.size) {
            return FeedSource.HOTBAR
        }
        return FeedSource.entries[ord]
    }

    /** Writes feed source selection to item metadata. */
    private fun feedSource(itemStack: ItemStack, feedSource: FeedSource) {
        itemStack.editMeta { meta: ItemMeta ->
            meta.persistentDataContainer.set(
                FEED_SOURCE,
                PersistentDataType.INTEGER,
                feedSource.ordinal,
            )
        }
    }

    /** Rebuilds lore lines to show the active feed source selection. */
    private fun updateLore(itemStack: ItemStack) {
        val lore = itemStack.lore()?.toMutableList() ?: mutableListOf()

        // Remove previous trowel-generated lore and append updated state line.
        lore.removeIf { component: Component? -> isTrowelLore(component) }

        val feedSource = feedSource(itemStack)
        val langLore = langLore ?: return
        lore +=
            langLore.format("§a$feedSource").mapNotNull { line ->
                line?.let { ItemUtil.addSentinel(it, SENTINEL) }
            }

        itemStack.lore(lore)
    }

    /** Cycles feed source when right-clicking the trowel in inventory. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerClickInventory(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }

        // Only handle right-click pickup-half with empty cursor.
        if (event.action != InventoryAction.PICKUP_HALF || (event.cursor.type != Material.AIR)) {
            return
        }

        val item = event.currentItem ?: return
        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
        if (customItem !is Trowel || !customItem.enabled()) {
            return
        }

        // Rotate to next configured feed source and persist lore.
        val feedSource = feedSource(item)
        feedSource(item, feedSource.next())
        updateLore(item)

        val player = event.whoClicked as Player
        player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f)
        event.isCancelled = true
    }

    /** Attempts to place a random block from selected feed source on right-click block. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractBlock(event: PlayerInteractEvent) {
        // Require right-click block with main hand.
        if (
            !event.hasBlock() ||
                event.action != Action.RIGHT_CLICK_BLOCK ||
                event.hand != EquipmentSlot.HAND
        ) {
            return
        }

        // Require trowel in main hand.
        val player = event.player
        val itemInHand = player.equipment.getItem(EquipmentSlot.HAND)
        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(itemInHand)
        if (customItem !is Trowel || !customItem.enabled()) {
            return
        }

        // Prevent follow-up offhand interaction on same click.
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        // Select random placeable block from configured source and attempt placement.
        val block = event.clickedBlock
        val inventory = player.inventory
        val fedSource = feedSource(itemInHand)
        val possibleSlots = fedSource.slots.clone()
        val module = module ?: return
        val itemRegistry = module.core?.itemRegistry()
        var count = possibleSlots.size
        while (count > 0) {
            val index: Int = random.nextInt(count)
            val itemStack = inventory.getItem(possibleSlots[index])
            // Skip empty/non-block/shulker slots.
            if (
                itemStack == null ||
                    !itemStack.type.isBlock ||
                    Tag.SHULKER_BOXES.isTagged(itemStack.type)
            ) {
                // Remove rejected slot from random pool by swapping with active tail.
                possibleSlots[index] = possibleSlots[--count]
                continue
            }
            val customItemSlot: CustomItem? = itemRegistry?.get(itemStack)
            // Never place custom items.
            if (customItemSlot != null) {
                possibleSlots[index] = possibleSlots[--count]
                continue
            }

            val nmsItem = Nms.itemHandle(itemStack)
            val nmsPlayer = Nms.playerHandle(player) ?: return
            val nmsWorld = Nms.worldHandle(player.world)

            // Build NMS placement context to preserve vanilla placement rules.
            val direction = CraftBlock.blockFaceToNotch(event.blockFace) ?: return
            val clickedBlock = block ?: return
            val blockPos = BlockPos(clickedBlock.x, clickedBlock.y, clickedBlock.z)
            val interactionPoint = event.interactionPoint ?: return
            val hitPos = Vec3(interactionPoint.x, interactionPoint.y, interactionPoint.z)
            val blockHitResult = BlockHitResult(hitPos, direction, blockPos, false)
            val itemHandle = nmsItem ?: return
            val amountPre = itemHandle.count
            val actionContext =
                UseOnContext(
                    nmsWorld,
                    nmsPlayer,
                    InteractionHand.MAIN_HAND,
                    itemHandle,
                    blockHitResult,
                )

            // Resolve placement sound before possible item consumption.
            var soundType: SoundType? = null
            if (itemHandle.item is BlockItem) {
                val blockItem = itemHandle.item as BlockItem
                val placeState: BlockState? =
                    blockItem.block.getStateForPlacement(BlockPlaceContext(actionContext))
                soundType = placeState?.soundType
            }

            // Execute placement through NMS for full vanilla behavior parity.
            val result = itemHandle.useOn(actionContext)

            // Creative mode should not consume source stacks.
            if (player.gameMode == GameMode.CREATIVE) {
                itemHandle.count = amountPre
            }

            if (result.consumesAction()) {
                PlayerUtil.swingArm(player, EquipmentSlot.HAND)
                ItemUtil.damageItem(player, itemInHand, 1)
                if (soundType != null) {
                    nmsWorld.playSound(
                        null,
                        blockPos,
                        soundType.placeSound,
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0f) / 2.0f,
                        soundType.getPitch() * 0.8f,
                    )
                }
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(nmsPlayer, blockPos, itemHandle)
            }

            nmsPlayer.connection.send(ClientboundBlockUpdatePacket(nmsWorld, blockPos))
            nmsPlayer.connection.send(
                ClientboundBlockUpdatePacket(nmsWorld, blockPos.relative(direction))
            )
            return
        }

        // No eligible block item found in the selected feed source.
        player.playSound(
            player,
            Sound.UI_STONECUTTER_SELECT_RECIPE,
            SoundCategory.MASTER,
            1.0f,
            2.0f,
        )
    }

    /** Updates lore whenever the trowel stack is rebuilt. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        updateLore(itemStack)
        return itemStack
    }

    /** Prevents conflicting vanilla interactions for the trowel item. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
    }

    companion object {
        /** Sentinel key used to identify lore lines generated by the trowel. */
        private val SENTINEL = StorageUtil.namespacedKey("vane", "trowel_lore")

        /** Metadata key storing the selected feed source ordinal. */
        val FEED_SOURCE: NamespacedKey = StorageUtil.namespacedKey("vane", "feed_source")

        /** Deterministic random source used for slot selection. */
        private val random = Random(23584982345L)

        /** Returns whether the given component belongs to trowel-generated lore. */
        private fun isTrowelLore(component: Component?): Boolean {
            return ItemUtil.hasSentinel(component, SENTINEL)
        }
    }
}
