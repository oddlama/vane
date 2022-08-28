package org.oddlama.vane.core.item;

import java.util.ArrayList;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.text.Component;

public class DurabilityManager extends Listener<Core> {
	public static final NamespacedKey ITEM_DURABILITY_MAX = StorageUtil.namespaced_key("vane", "durability.max");
	public static final NamespacedKey ITEM_DURABILITY_DAMAGE = StorageUtil.namespaced_key("vane", "durability.damage");

	private static final NamespacedKey SENTINEL = StorageUtil.namespaced_key("vane", "durability_override_lore");

	public DurabilityManager(final Context<Core> context) {
		super(context);
	}

	/**
	 * Returns true if the given component is associated to our custom durabiltiy.
	 */
	private static boolean is_durability_lore(final Component component) {
		return ItemUtil.has_sentinel(component, SENTINEL);
	}

	/**
	 * Removes associated lore from an item.
	 */
	private static void remove_lore(final ItemStack item_stack) {
		final var lore = item_stack.lore();
		if (lore != null) {
			lore.removeIf(DurabilityManager::is_durability_lore);
			if (lore.size() > 0) {
				item_stack.lore(lore);
			} else {
				item_stack.lore(null);
			}
		}
	}

	/**
	 * Returns the remaining uses for a given item, if it has custom durability.
	 * Returns -1 otherwise.
	 */
	private static int remaining_uses(final ItemStack item_stack) {
		if (item_stack == null || !item_stack.hasItemMeta()) {
			return -1;
		}
		final var data = item_stack.getItemMeta().getPersistentDataContainer();
		final var max = data.getOrDefault(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, -1);
		if (max == -1) {
			return -1;
		}
		final var damage = data.getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, 0);
		return Math.max(0, Math.min(max - damage, max));
	}

	/**
	 * Updates lore on an item. Both persistent data field should exist,
	 * otherwise result is unspecified.
	 */
	private static void update_lore(final CustomItem custom_item, final ItemStack item_stack) {
		var lore = item_stack.lore();
		if (lore == null) {
			lore = new ArrayList<Component>();
		}

		// Remove old component
		lore.removeIf(DurabilityManager::is_durability_lore);

		// Add new component only if requested
		final var lore_component = custom_item.durabilityLore();
		if (lore_component != null) {
			final var max = item_stack.getItemMeta().getPersistentDataContainer().getOrDefault(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, custom_item.durability());
			final var remaining_uses = remaining_uses(item_stack);
			lore.add(ItemUtil.add_sentinel(lore_component.args(Component.text(remaining_uses), Component.text(max)), SENTINEL));
		}

		item_stack.lore(lore);
	}

	/**
	 * Calculates visual damage value given the visual max durability,
	 * the actual damage and actual maximum damage. The actual damage value
	 * will be automatically clamped to reasonable values.
	 */
	private static int visual_damage(final int actual_damage, final int actual_max, final short visual_max) {
		if (actual_damage <= 0) {
			return 0;
		} else if (actual_damage == actual_max - 1) {
			// This forces item's that have 1 durability left to also show 1 durability left
			// in their visual damage, which allows client mods to swap items if necessary.
			return visual_max - 1;
		} else if (actual_damage >= actual_max) {
			return visual_max;
		} else {
			final var damage_percentage = (double)actual_damage / actual_max;
			// Never allow the calculation to show the item as 0 visual durability,
			return Math.min(visual_max - 1, (int)(damage_percentage * visual_max));
		}
	}

	/**
	 * Sets the item's damage regarding our custom durability. The durability will get
	 * clamped to plausible values. Damage values >= max will result in item breakage.
	 * The maximum value will be taken from the item tag if it exists.
	 */
	private static void set_damage_and_update_item(final CustomItem custom_item, final ItemStack item_stack, int damage) {
		// Honor unbreakable flag
		final var ro_meta = item_stack.getItemMeta();
		if (ro_meta.isUnbreakable()) {
			damage = 0;
		}

		// Clamp values below or above defined damage values.
		final var actual_max = ro_meta.getPersistentDataContainer().getOrDefault(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, custom_item.durability());
		final var actual_damage = Math.min(actual_max, Math.max(0, damage));

		// Store new damage values
		item_stack.editMeta(meta -> {
			final var data = meta.getPersistentDataContainer();
			data.set(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, actual_damage);
			if (!data.has(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER)) {
				data.set(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, actual_max);
			}
		});

		// Update lore
		update_lore(custom_item, item_stack);

		// Update visual damage if base item is damageable.
		item_stack.editMeta(Damageable.class, damage_meta -> {
			final var visual_max = item_stack.getType().getMaxDurability();
			damage_meta.setDamage(visual_damage(actual_damage, actual_max, visual_max));
		});
	}

	/**
	 * Initialized damage tags on the item, or removes them if custom durability
	 * is disabled for the given custom item.
	 */
	public static boolean initialize_or_update_max(final CustomItem custom_item, final ItemStack item_stack) {
		// Remember damage if set.
		var old_damage = item_stack.getItemMeta().getPersistentDataContainer().getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, -1);

		// First, remove all components.
		item_stack.editMeta(meta -> {
			final var data = meta.getPersistentDataContainer();
			data.remove(ITEM_DURABILITY_DAMAGE);
			data.remove(ITEM_DURABILITY_MAX);
		});

		// The item has no durability anymore. Remove leftover lore and return.
		if (custom_item.durability() <= 0) {
			remove_lore(item_stack);
			return false;
		}

		final int actual_damage;
		if (old_damage == -1) {
			if (item_stack.getItemMeta() instanceof final Damageable damage_meta) {
				// If there was no old damage value, initialize proportionally by visual damage.
				final var visual_max = item_stack.getType().getMaxDurability();
				final var damage_percentage = (double)damage_meta.getDamage() / visual_max;
				actual_damage = (int)(custom_item.durability() * damage_percentage);
			} else {
				// There was no old damage value, but the item has no visual durability.
				// Initialize with max durability.
				actual_damage = 0;
			}
		} else {
			// Keep old damage.
			actual_damage = old_damage;
		}

		set_damage_and_update_item(custom_item, item_stack, actual_damage);
		return true;
	}

	/**
	 * Damages item by given amount regarding our custom durability.
	 * Negative amounts repair the item.
	 */
	private static void damage_and_update_item(final CustomItem custom_item, final ItemStack item_stack, final int amount) {
		if (!item_stack.getItemMeta().getPersistentDataContainer().has(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER)) {
			if (!initialize_or_update_max(custom_item, item_stack)) {
				return;
			}
		}

		final var damage = item_stack.getItemMeta().getPersistentDataContainer().getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, 0);
		set_damage_and_update_item(custom_item, item_stack, damage + amount);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_item_damage(final PlayerItemDamageEvent event) {
		final var item = event.getItem();
		final var custom_item = get_module().item_registry().get(item);

		// Ignore normal items or custom items with standard durability.
		if (custom_item == null || custom_item.durability() <= 0) {
			return;
		}

		damage_and_update_item(custom_item, item, event.getDamage());

		// Wow this is hacky but the only workaround to prevent recusivly
		// calling this event. We always increase the visual durability by 1
		// and let the server implementation decrease it again to
		// allow the item to break.
		item.editMeta(Damageable.class, damage_meta -> damage_meta.setDamage(damage_meta.getDamage() - 1));
		event.setDamage(1);
	}


	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_item_mend(final PlayerItemMendEvent event) {
		// TODO: Durability Overridden items can not yet support mending effectively.
		// see: https://github.com/PaperMC/Paper/issues/7313
		final var item = event.getItem();
		final var custom_item = get_module().item_registry().get(item);

		// Ignore normal items or custom items with standard durability.
		if (custom_item == null || custom_item.durability() <= 0) {
			return;
		}

		final var mend_amount = 2 * event.getExperienceOrb().getExperience();
		damage_and_update_item(custom_item, item, -mend_amount);

		// Never let the server do any repairing, our durability bar is our percentage indicator.
		event.setCancelled(true);
	}

	// TODO: what about inventory based item repair?
	// Update durability on result items.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_prepare_anvil(final PrepareAnvilEvent event) {
		if (event.getResult() == null) {
			return;
		}

		final var r = event.getResult();
		final var custom_item_r = get_module().item_registry().get(r);
		if (custom_item_r == null) {
			return;
		}

		if (custom_item_r.durability() <= 0) {
			// Remove leftover components
			damage_and_update_item(custom_item_r, r, 0);
		} else {
			final var uses_a = remaining_uses(event.getInventory().getFirstItem());
			final var uses_b = remaining_uses(event.getInventory().getSecondItem());

			final var max = r.getItemMeta().getPersistentDataContainer().getOrDefault(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, custom_item_r.durability());
			if (uses_a >= 0 && uses_b >= 0) {
				set_damage_and_update_item(custom_item_r, r, max - (uses_a + uses_b));
			}
		}

		event.setResult(r);
	}
}
