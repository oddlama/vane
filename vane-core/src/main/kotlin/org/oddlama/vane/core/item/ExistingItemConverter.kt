package org.oddlama.vane.core.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.inventory.meta.Damageable
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.CustomItemHelper.customItemTagsFromItemStack
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context

/**
 * Converts legacy or outdated item stacks in inventories, dropped items, item frames and entity
 * equipment to current custom-item formats.
 *
 * @param context listener context.
 */
class ExistingItemConverter(context: Context<Core?>) :
    Listener<Core?>(context.namespace("existing_item_converter")) {
    /** Resolves legacy model data ids to currently registered custom items. */
    private fun fromOldItem(itemStack: ItemStack): CustomItem? {
        val modelDataInt =
            itemStack.itemMeta.customModelDataComponent.floats.firstOrNull()?.toInt() ?: return null
        val key = legacyItemKey(modelDataInt) ?: return null
        return module!!.itemRegistry()?.get(key)
    }

    /**
     * Compares item names by translation key or plain text, since components read back from a stack
     * may not be structurally equal to the ones written to it.
     */
    private fun sameName(a: Component?, b: Component?): Boolean {
        if (a is TranslatableComponent || b is TranslatableComponent) {
            return (a as? TranslatableComponent)?.key() == (b as? TranslatableComponent)?.key()
        }
        val plain = PlainTextComponentSerializer.plainText()
        return a?.let(plain::serialize) == b?.let(plain::serialize)
    }

    /** Result of converting a single stack; [stack] is null when the item should be removed. */
    private class Converted(val stack: ItemStack?)

    /**
     * Migrates a single item stack and any items stored inside it (shulker boxes, bundles). Returns
     * null if the stack is already up to date.
     */
    private fun convertStack(item: ItemStack): Converted? {
        val converted = convertSelf(item)
        val stack = if (converted == null) item else converted.stack ?: return converted
        return convertContents(stack)?.let(::Converted) ?: converted
    }

    /**
     * Migrates the items stored inside a shulker box or bundle. Returns null if nothing changed.
     */
    private fun convertContents(item: ItemStack): ItemStack? {
        if (!item.hasItemMeta()) return null

        when (val meta = item.itemMeta) {
            is BlockStateMeta -> {
                if (!meta.hasBlockState()) return null
                val shulkerBox = meta.blockState as? ShulkerBox ?: return null
                if (!processInventory(shulkerBox.snapshotInventory)) return null
                meta.blockState = shulkerBox
                return item.clone().apply { itemMeta = meta }
            }
            is BundleMeta -> {
                var changed = false
                val items =
                    meta.items.mapNotNull { stack ->
                        val converted = convertStack(stack) ?: return@mapNotNull stack
                        changed = true
                        converted.stack
                    }
                if (!changed) return null
                meta.setItems(items)
                return item.clone().apply { itemMeta = meta }
            }
            else -> return null
        }
    }

    /** Migrates a single item stack, ignoring its contents. */
    private fun convertSelf(item: ItemStack): Converted? {
        if (!item.hasItemMeta()) return null

        val customItem = module!!.itemRegistry()?.get(item)
        if (customItem == null) {
            val convertToCustomItem = fromOldItem(item) ?: return null
            val converted = convertToCustomItem.convertExistingStack(item) ?: return null
            converted.editMeta { it.itemName(convertToCustomItem.displayName()) }
            module!!.enchantmentManager?.updateEnchantedItem(converted)
            module!!.log.info("Converted legacy item to ${convertToCustomItem.key()}")
            return Converted(converted)
        }

        if (module!!.itemRegistry()?.shouldRemove(customItem.key()) == true) {
            module!!.log.info("Removed obsolete item ${customItem.key()}")
            return Converted(null)
        }

        val keyAndVersion = customItemTagsFromItemStack(item)
        val meta = item.itemMeta
        val modelDataInt = meta.customModelDataComponent.floats.firstOrNull()?.toInt()

        if (
            modelDataInt == null ||
                modelDataInt != customItem.customModelData() ||
                meta.itemModel != customItem.itemModel() ||
                !sameName(meta.itemName(), customItem.displayName()) ||
                item.type != customItem.baseMaterial() ||
                keyAndVersion?.second != customItem.version()
        ) {
            module!!.log.info("Updated item ${customItem.key()}")
            return Converted(customItem.convertExistingStack(item))
        }

        val damageableMeta = meta as Damageable
        val maxDamage =
            if (damageableMeta.hasMaxDamage()) damageableMeta.maxDamage
            else item.type.maxDurability.toInt()
        val correctMaxDamage =
            if (customItem.durability() == 0) item.type.maxDurability.toInt()
            else customItem.durability()

        if (
            maxDamage != correctMaxDamage ||
                meta.persistentDataContainer.has(DurabilityManager.ITEM_DURABILITY_DAMAGE)
        ) {
            module!!.log.info("Updated item durability ${customItem.key()}")
            val updated = item.clone()
            DurabilityManager.updateDamage(customItem, updated)
            return Converted(updated)
        }

        return null
    }

    /** Processes and migrates all item stacks in an inventory. Returns whether anything changed. */
    private fun processInventory(inventory: Inventory): Boolean {
        val contents = inventory.contents
        var changed = 0

        for (i in contents.indices) {
            val item = contents[i] ?: continue
            val converted = convertStack(item) ?: continue
            contents[i] = converted.stack
            ++changed
        }

        if (changed > 0) inventory.contents = contents
        return changed > 0
    }

    /** Migrates dropped items, item frame contents and entity equipment. */
    private fun processEntity(entity: Entity) {
        when (entity) {
            is Item -> {
                val converted = convertStack(entity.itemStack) ?: return
                val stack = converted.stack
                if (stack == null) entity.remove() else entity.itemStack = stack
            }
            is ItemFrame -> {
                val converted = convertStack(entity.item) ?: return
                entity.setItem(converted.stack, false)
            }
            // Players are handled through their inventory on join.
            is LivingEntity -> if (entity !is Player) processEquipment(entity)
        }
    }

    /** Migrates the equipment of mobs and armor stands. */
    private fun processEquipment(entity: LivingEntity) {
        val equipment = entity.equipment ?: return
        for (slot in EquipmentSlot.entries) {
            if (!entity.canUseEquipmentSlot(slot)) continue
            val converted = convertStack(equipment.getItem(slot)) ?: continue
            equipment.setItem(slot, converted.stack, true)
        }
    }

    /** Converts items in the player inventory on join. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        processInventory(event.player.inventory)
    }

    /** Converts items in opened inventories. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        processInventory(event.inventory)
    }

    /** Converts dropped items, item frame contents and entity equipment when their chunk loads. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntitiesLoad(event: EntitiesLoadEvent) = event.entities.forEach(::processEntity)

    companion object {
        /**
         * Custom model data of items from before vane had persistent item identifiers, and the
         * items they became. These ids were never reused, so they cannot be derived from the model
         * data that items are registered with today.
         */
        private val LEGACY_MODEL_DATA: Map<Int, String> = buildMap {
            put(7758190, "vane_trifles:wooden_sickle")
            put(7758191, "vane_trifles:stone_sickle")
            put(7758192, "vane_trifles:iron_sickle")
            put(7758193, "vane_trifles:golden_sickle")
            put(7758194, "vane_trifles:diamond_sickle")
            put(7758195, "vane_trifles:netherite_sickle")
            for (id in 7758254..7758259) put(id, "vane_trifles:file")
            put(7758318, "vane_trifles:empty_xp_bottle")
            put(7758382, "vane_trifles:small_xp_bottle")
            put(7758383, "vane_trifles:medium_xp_bottle")
            put(7758384, "vane_trifles:large_xp_bottle")
            put(7758446, "vane_trifles:home_scroll")
            put(7758510, "vane_trifles:unstable_scroll")
            put(7758574, "vane_trifles:reinforced_elytra")
            put(7823726, "vane_enchantments:ancient_tome")
            put(7823727, "vane_enchantments:enchanted_ancient_tome")
            put(7823790, "vane_enchantments:ancient_tome_of_knowledge")
            put(7823791, "vane_enchantments:enchanted_ancient_tome_of_knowledge")
            put(7823854, "vane_enchantments:ancient_tome_of_the_gods")
            put(7823855, "vane_enchantments:enchanted_ancient_tome_of_the_gods")
        }

        /**
         * Returns the key of the item that legacy custom model data [modelData] became, or `null`
         * if it is not a legacy id.
         */
        fun legacyItemKey(modelData: Int): NamespacedKey? =
            LEGACY_MODEL_DATA[modelData]?.let(NamespacedKey::fromString)
    }
}
