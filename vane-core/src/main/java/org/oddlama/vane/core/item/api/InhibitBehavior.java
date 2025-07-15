package org.oddlama.vane.core.item.api;

public enum InhibitBehavior {
    /**
     * Prevents the item from being used as an ingredient in any (recognized) vanilla crafting
     * recipe, i.e., recipe in the "minecraft" namespace.
     */
    USE_IN_VANILLA_RECIPE,
    /**
     * Prevents the item from causing any entities to target and walk to the player when the item is
     * held in the main or offhand.
     */
    TEMPT,
    /** Prevents the item from burning. */
    ITEM_BURN,
    /** Prevents hoes from creating farmland. */
    HOE_TILL,
    /** Prevents players from using the offhand item when holding this in the main hand. */
    USE_OFFHAND,
    /**
     * Prevents players from mending this item (and prevents it from gaining mending in the anvil).
     */
    MEND,
    /** Prevents players from adding new enchantments via the anvil. */
    NEW_ENCHANTS,
    /** Prevents dispensers from dispense this item */
    DISPENSE,
}
