package org.oddlama.vane.core.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;

public class ExistingItemConverter extends Listener<Core> {
	public ExistingItemConverter(final Context<Core> context) {
		super(context.namespace("existing_item_converter"));
	}

	private CustomItem from_old_item(final ItemStack item_stack) {
		final var meta = item_stack.getItemMeta();
		if (meta == null || !meta.hasCustomModelData()) {
			return null;
		}

		// If lookups fail, we return null and nothing will be done.
		String new_item_key = null;
		switch (meta.getCustomModelData()) {
			case 7758190: new_item_key = "vane_trifles:wooden_sickle"; break;
			case 7758191: new_item_key = "vane_trifles:stone_sickle"; break;
			case 7758192: new_item_key = "vane_trifles:iron_sickle"; break;
			case 7758193: new_item_key = "vane_trifles:golden_sickle"; break;
			case 7758194: new_item_key = "vane_trifles:diamond_sickle"; break;
			case 7758195: new_item_key = "vane_trifles:netherite_sickle"; break;
			case 7758254: // fallthrough
			case 7758255: // fallthrough
			case 7758256: // fallthrough
			case 7758257: // fallthrough
			case 7758258: // fallthrough
			case 7758259: new_item_key = "vane_trifles:file"; break;
			case 7758318: new_item_key = "vane_trifles:empty_xp_bottle"; break;
			case 7758382: new_item_key = "vane_trifles:small_xp_bottle"; break;
			case 7758383: new_item_key = "vane_trifles:medium_xp_bottle"; break;
			case 7758384: new_item_key = "vane_trifles:large_xp_bottle"; break;
			case 7758446: new_item_key = "vane_trifles:home_scroll"; break;
			case 7758510: new_item_key = "vane_trifles:unstable_scroll"; break;
			case 7758574: new_item_key = "vane_trifles:reinforced_elytra"; break;
			case 7823726: new_item_key = "vane_enchantments:ancient_tome"; break;
			case 7823727: new_item_key = "vane_enchantments:enchanted_ancient_tome"; break;
			case 7823790: new_item_key = "vane_enchantments:ancient_tome_of_knowledge"; break;
			case 7823791: new_item_key = "vane_enchantments:enchanted_ancient_tome_of_knowledge"; break;
			case 7823854: new_item_key = "vane_enchantments:ancient_tome_of_the_gods"; break;
			case 7823855: new_item_key = "vane_enchantments:enchanted_ancient_tome_of_the_gods"; break;
		}

		if (new_item_key == null) {
			return null;
		}
		return get_module().item_registry().get(NamespacedKey.fromString(new_item_key));
	}

	private void process_inventory(@NotNull Inventory inventory) {
		final var contents = inventory.getContents();
		int changed = 0;

		for (int i = 0; i < contents.length; ++i) {
			final var is = contents[i];
			if (is == null || !is.hasItemMeta()) {
				continue;
			}

			final var custom_item = get_module().item_registry().get(is);
			if (custom_item == null) {
				// Determine if the item stack should be converted to a custom item from a legacy definition
				final var convert_to_custom_item = from_old_item(is);
				if (convert_to_custom_item == null) {
					continue;
				}

				contents[i] = convert_to_custom_item.convertExistingStack(is);
				contents[i].editMeta(meta -> meta.displayName(convert_to_custom_item.displayName()));
				get_module().enchantment_manager.update_enchanted_item(contents[i]);
				get_module().log.info("Converted legacy item to " + convert_to_custom_item.key());
				++changed;
				continue;
			}

			// Remove obsolete custom items
			if (get_module().item_registry().shouldRemove(custom_item.key())) {
				contents[i] = null;
				get_module().log.info("Removed obsolete item " + custom_item.key());
				++changed;
				continue;
			}

			// Update custom items to new version, or if another detectable property changed.
			final var key_and_version = CustomItemHelper.customItemTagsFromItemStack(is);
			final var meta = is.getItemMeta();
			if (meta.getCustomModelData() != custom_item.customModelData() ||
				is.getType() != custom_item.baseMaterial() ||
				key_and_version.getRight() != custom_item.version()
			) {
				// Also includes durability max update.
				contents[i] = custom_item.convertExistingStack(is);
				get_module().log.info("Updated item " + custom_item.key());
				++changed;
				continue;
			}

			// Update maximum durability on existing items if changed.
			if (meta.getPersistentDataContainer().getOrDefault(DurabilityManager.ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, 0) != custom_item.durability()) {
				get_module().log.info("Updated item durability " + custom_item.key());
				DurabilityManager.initialize_or_update_max(custom_item, contents[i]);
				++changed;
			}
		}

		if (changed > 0) {
			inventory.setContents(contents);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		process_inventory(event.getPlayer().getInventory());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_inventory_open(final InventoryOpenEvent event) {
		// Catches enderchests, and inventories by other plugins
		process_inventory(event.getInventory());
	}
}
