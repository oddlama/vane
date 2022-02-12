package org.oddlama.vane.core.itemv2.api;

public enum InhibitBehavior {
	/**
	 * Prevents the item from being used as an ingredient in
	 * any vanilla crafting recipe (recipe in "minecraft namespace").
	 */
	USE_IN_VANILLA_CRAFTING_RECIPE,
	/**
	 * Prevents the item from being used as an ingredient in
	 * any smithing recipe. To enable smithing custom items,
	 * add a dummy recipe to trigger the event and override the result
	 * in PrepareSmithingEvent, with a priority of EventPriority.LOW or higher.
	 */
	USE_IN_SMITHING_RECIPE,
	/**
	 * Prevents the item from causing any entities to target and walk to
	 * the player when the item is held in the main or offhand.
	 */
	TEMPT,
	/**
	 * Prevents the item from burning.
	 */
	ITEM_BURN,
	/**
	 * Prevents hoes from creating farmland.
	 */
	HOE_TILL,
}
