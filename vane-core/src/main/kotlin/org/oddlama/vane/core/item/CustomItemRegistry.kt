package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.CustomItemRegistry
import org.oddlama.vane.core.item.api.CustomModelDataRegistry

/** Runtime registry for all loaded custom items. */
class CustomItemRegistry : CustomItemRegistry {
    /** Registered custom items keyed by namespaced key. */
    private val items = mutableMapOf<NamespacedKey, CustomItem>()

    /** Keys scheduled for permanent item removal. */
    private val itemsToRemove = mutableSetOf<NamespacedKey>()

    /** Shared model data registry. */
    private val modelDataRegistry: CustomModelDataRegistry? = Core.instance()?.modelDataRegistry()

    /** Returns whether an item key is registered. */
    override fun has(resourceKey: NamespacedKey): Boolean = resourceKey in items

    /** Returns all registered custom items. */
    override fun all(): Collection<CustomItem> = items.values

    /** Returns a custom item by key. */
    override fun get(resourceKey: NamespacedKey): CustomItem? = items[resourceKey]

    /** Returns a custom item from stack tags. */
    override fun get(itemStack: ItemStack?): CustomItem? {
        val keyAndVersion = CustomItemHelper.customItemTagsFromItemStack(itemStack) ?: return null
        val key = keyAndVersion.first ?: return null
        return get(key)
    }

    /** Registers a custom item and reserves its model data. */
    override fun register(customItem: CustomItem) {
        modelDataRegistry?.reserveSingle(customItem.key(), customItem.customModelData())
        require(!has(customItem.key())) {
            "A custom item with the same key '${customItem.key()}' has already been registered"
        }
        items[customItem.key()] = customItem
    }

    /** Marks an item key for permanent removal. */
    override fun removePermanently(key: NamespacedKey) {
        itemsToRemove.add(key)
    }

    /** Returns whether an item key should be removed. */
    override fun shouldRemove(key: NamespacedKey): Boolean = key in itemsToRemove

    /** Returns the model data registry. */
    override fun dataRegistry(): CustomModelDataRegistry =
        requireNotNull(modelDataRegistry) {
            "CustomModelDataRegistry is not available"
        }
}
