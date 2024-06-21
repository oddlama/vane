package org.oddlama.vane.core.item;

import java.util.ArrayList;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.text.Component;


// TODO: what about inventory based item repair?

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
		final int finalDamage = damage;
		item_stack.editMeta(Damageable.class, meta -> {
			meta.setDamage(finalDamage);
			meta.setMaxDamage(null);
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


	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_item_damage(final PlayerItemDamageEvent event) {
		final var item = event.getItem();
		final var custom_item = get_module().item_registry().get(item);

		// Ignore normal items or custom items with standard durability.
		if (custom_item == null || custom_item.durability() == 0) {
			return;
		}

		if(remove_old_damage(custom_item, item)){
			item.editMeta(Damageable.class, damage_meta ->
				damage_meta.setDamage(Math.max(0, damage_meta.getDamage() - event.getDamage())));

		};
	}

	/**
	 * Update existing max damage to match the configuration
	 */
	static public boolean update_damage(CustomItem custom_item, ItemStack item_stack) {
		if (!(item_stack.getItemMeta() instanceof Damageable meta))
			return false; // everything should be damageable now
		int old_max_damage = meta.hasMaxDamage() ? meta.getMaxDamage() : item_stack.getType().getMaxDurability();
		int old_damage = meta.hasDamage() ? meta.getDamage() : old_max_damage;
		float percentage = (float) old_damage / (float) old_max_damage;

		final int max_damage = custom_item.durability() != 0
			? custom_item.durability() 
			: item_stack.getType().getMaxDurability();

		if (old_max_damage == max_damage) return false; // same max damage, so no need to change it

		final int damage = Math.round(max_damage * percentage);
		return item_stack.editMeta(Damageable.class, itemMeta -> {
			itemMeta.setDamage(0);
			itemMeta.setMaxDamage(max_damage);
			itemMeta.setDamage(Math.min(damage, max_damage));
		});

		// int oldDamage = meta.getPersistentDataContainer().get(ITEM_DURABILITY_DAMAGE,
		// PersistentDataType.INTEGER);
	}

	/**
	 * Replace custom durability mechanic with vanilla one
	 */
	static public boolean remove_old_damage(CustomItem custom_item, ItemStack item_stack) {
		if (custom_item == null) return false;
		if (!(item_stack.getItemMeta() instanceof Damageable meta)) return false;
		
		PersistentDataContainer data = meta.getPersistentDataContainer();
		if(!data.has(ITEM_DURABILITY_DAMAGE) && !data.has(ITEM_DURABILITY_MAX)) return false;

		// get old values stored in the data
		final int oldDamage = data.getOrDefault(ITEM_DURABILITY_DAMAGE, PersistentDataType.INTEGER, 0);
		final int oldMaxDamage = data.getOrDefault(ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, custom_item.durability());

		final int newDamage = oldMaxDamage == custom_item.durability()
			? oldDamage // to avoid precision errors on bigger numbers
			: Math.round(custom_item.durability() * ((float) oldDamage / (float) oldMaxDamage));			

		remove_lore(item_stack);

		return item_stack.editMeta(Damageable.class, itemMeta -> {
			if(custom_item.durability() != 0) {
				itemMeta.setMaxDamage(custom_item.durability());
			} else {
				itemMeta.setMaxDamage((int) item_stack.getType().getMaxDurability());
			}	
			itemMeta.setDamage(newDamage);
			itemMeta.getPersistentDataContainer().remove(ITEM_DURABILITY_DAMAGE);
			itemMeta.getPersistentDataContainer().remove(ITEM_DURABILITY_MAX);
		});

	}
}
