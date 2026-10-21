package org.oddlama.vane.core.item.api

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core

/** This is the registry with which you can register your custom items. */
interface CustomItemRegistry {
    /** Returns true if a custom item with the given resourceKey has been registered. */
    fun has(resourceKey: NamespacedKey): Boolean

    /** Returns all registered custom items. */
    fun all(): Collection<CustomItem>

    /**
     * Tries to retrieve a custom item definition by resource key. Returns null if no such
     * definition exists.
     */
    fun get(resourceKey: NamespacedKey): CustomItem?

    /**
     * Tries to retrieve a custom item definition from an ItemStack. Returns null if the itemstack
     * is not a custom item, or references a custom item that has not been registered (e.g.,
     * previously installed plugin).
     */
    fun get(itemStack: ItemStack?): CustomItem?

    /**
     * Registers a new custom item. Throws an IllegalArgumentException if an item with the same key
     * has already been registered.
     */
    fun register(customItem: CustomItem)

    /**
     * Queues removal of a given custom item. If any matching item is encountered in the future, it
     * will be removed permanently from the respective inventory.
     *
     * This is not a one-off operation! Removal only actually happens when the item is encountered
     * due to a player interacting with an inventory. This is intended as a way for plugins to queue
     * removal of items from old plugin versions.
     */
    fun removePermanently(key: NamespacedKey)

    /** Returns true if the associated key was queued for removal using [removePermanently]. */
    fun shouldRemove(key: NamespacedKey): Boolean

    /** Returns the custom model data registry. */
    fun dataRegistry(): CustomModelDataRegistry

    /** Global registry access helpers. */
    companion object {
        /** Retrieves the global registry instance from the running vane-core instance, if any. */
        @JvmStatic fun instance(): CustomItemRegistry? = Core.instance()?.itemRegistry()
    }
}
