package org.oddlama.vane.core.item.api;

import java.io.IOException;
import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.item.CustomItemHelper;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * This is the CustomItem specification that all custom items must implement to be registered with the vane custom-item API.
 */
public interface CustomItem {
	/**
	 * The unique identifier for this custom item. This is the only value that can never be changed after initial item registration.
	 * If you want to deprecate an item, you can force your subclass to always return false in {@link #enabled()}. This will cause existing
	 * items to behave as their base items. To completely remove a custom item from encountered inventories, you can
	 * queue it for removal using {@link CustomItemRegistry#removePermanently(NamespacedKey)}.
	 * The key is the primary identifier of an item and must never change after initial registration.
	 * All other properties can be safely changed between restarts and even on configuration reloads.
	 */
	public NamespacedKey key();

	/**
	 * Return true to indicate that this item is enabled. If this function returns false,
	 * the custom item will be regarded as nonexistent. This means that in this case,
	 * no item-stack updates are executed, and no related events are processed internally.
	 */
	public boolean enabled();

	/**
	 * Returns a version number of this item. Increasing this value will cause {@link #convertExistingStack(ItemStack)}
	 * to be called on any encountered itemstack of this custom item. Useful to force an update of existing
	 * items when you, for example, change the {@link #updateItemStack(ItemStack)} function to add custom
	 * properties to your custom item.
	 */
	public int version();

	/**
	 * Returns the base material that the custom item is made of.
	 * If this is changed, any encountered item will automatically be updated.
	 *
	 * To ensure that a breakage of the plugin never creates value for players,
	 * use a material with less net-worth than what the custom item provides.
	 * For crafting ingredients, we recommend using an item that has no other use than in crafting.
	 * Generally, it is a good idea to pick materials that have an item with similar properties.
	 *
	 * By default, no attempts will be made to remove the vanilla behavior of the base items,
	 * except for inhibiting use in crafting recipes that require the base material.
	 * See {@link #inhibitedBehaviors()} for more information. If you require other
	 * behaviors to be inhibited, you need to write your own event listeners.
	 */
	public Material baseMaterial();

	/**
	 * Returns the custom model data used as a selector in a resource pack.
	 * If this is changed, any encountered item will automatically be updated.
	 * We recommend reserving a set of ids using {@link CustomModelDataRegistry#reserveRange(NamespacedKey, int, int)} when your plugin starts.
	 * This allows you to freely use and re-use the registered ids without having to worry about clashes with other plugins.
	 */
	public int customModelData();


	/**
	 * Returns the display name for newly created items of this type.
	 * This will only NOT be updated on existing items. If you want that behavior, you
	 * can easily implement it by overriding {@link #updateItemStack(ItemStack)}.
	 * By using translatable components, there's no need to update this except when changing the translation key.
	 */
	public @Nullable Component displayName();

	/**
	 * A custom translation key that will be used to display durability on the item.
	 * Return null to disable durability lore. Arguments to the translatable component
	 * that will be supplied are the current durability (%1$s) and max durability (%2$s).
	 */
	default public @Nullable TranslatableComponent durabilityLore() {
		return Component.translatable("item.durability")
			.color(NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false);
	}

	/**
	 * The item's effective maximum durability. If this returns 0, no changes will be made to the base
	 * item's durability mechanic. If this is set to a value > 0, it requires a base item with durability.
	 * The durability bar of the base item then acts solely as an indicative value of a separately stored durability.
	 * Changes to the item's durability by classical means are automatically reflected in this property.
	 *
	 * If this value is changed while item stacks of this custom item already exist with a different maximum durability,
	 * the affected items will be updated and keep their current durability, but clamped to the new maximum.
	 */
	public int durability();

	/**
	 * Specifies which vanilla behaviors should be inhibited for this custom item.
	 *
	 * By default, any _vanilla_ crafting recipe (i.e. "minecraft" is the associated key's namespace)
	 * will be disabled when this custom item is used in the recipe's inputs, as well as any
	 * smithing recipe with this item as any input.
	 *
	 * So a custom item based on paper as the base material cannot be used to craft,
	 * for example,	* books (which require paper as an ingredient),
	 * or a diamond hoe-	* based item will not be converted to a vanilla netherite hoe in the smithing table.
	 *
	 * If you require a hoe that doesn't till blocks or a carrot/fungus on a stick
	 * that doesn't attract certain entities, you can override {@link #inhibitedBehaviors()}
	 * to specify what should be prohibited.
	 * The user must handle anything else.
	 */
	default public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE);
	}

	/**
	 * This function will be called when the resource pack is generated, and allows you
	 * to add the item's texture, translation strings or any other client side resources to a pack
	 * that can/will be distributed to players.
	 */
	public void addResources(final ResourcePackGenerator rp) throws IOException;

	/**
	 * This function will be called when a custom item of this type is newly created,
	 * or when an existing stack needs to be updated. This can include cases where
	 * no base information actually changed, but an item still is considered to
	 * be updated, for example, anvil results.
	 */
	default public ItemStack updateItemStack(@NotNull final ItemStack itemStack) {
		return itemStack;
	}

	/**
	 * Returns true if the given itemstack is an "instance" this custom item.
	 */
	default public boolean isInstance(@Nullable final ItemStack itemStack) {
		return CustomItemRegistry.instance().get(itemStack) == this;
	}

	public default ItemStack newStack() {
		return CustomItemHelper.newStack(this);
	}

	public default ItemStack newStack(final int amount) {
		return CustomItemHelper.newStack(this, amount);
	}

	public default ItemStack convertExistingStack(final ItemStack item_stack) {
		return CustomItemHelper.convertExistingStack(this, item_stack);
	}
}
