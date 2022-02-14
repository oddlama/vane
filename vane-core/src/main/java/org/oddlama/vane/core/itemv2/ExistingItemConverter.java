package org.oddlama.vane.core.itemv2;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.itemv2.api.CustomItem;
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
		switch (meta.getCustomModelData()) {
			// 7758190: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@7423691e with base material WOODEN_HOE and model_data
			case 7758190: return get_module().item_registry().get(NamespacedKey.fromString("vane_trifles:wooden_sickle"));
			// 7758191: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@7f49b82b with base material STONE_HOE and model_data
			// 7758192: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@39de5c61 with base material IRON_HOE and model_data
			// 7758193: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@3c7defc4 with base material GOLDEN_HOE and model_data
			// 7758194: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@53f91acc with base material DIAMOND_HOE and model_data
			// 7758195: org.oddlama.vane.trifles.items.Sickle variant org.oddlama.vane.trifles.items.Sickle$SickleVariant@61c36878 with base material NETHERITE_HOE and model_data
			// 7758254: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@1d9dcc5b with base material WOODEN_HOE and model_data
			// 7758255: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@8b31007 with base material STONE_HOE and model_data
			// 7758256: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@2e626887 with base material IRON_HOE and model_data
			// 7758257: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@53141b43 with base material GOLDEN_HOE and model_data
			// 7758258: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@2018ea6e with base material DIAMOND_HOE and model_data
			// 7758259: org.oddlama.vane.trifles.items.File variant org.oddlama.vane.trifles.items.File$FileVariant@303bf542 with base material NETHERITE_HOE and model_data
			// 7758318: org.oddlama.vane.trifles.items.EmptyXpBottle variant org.oddlama.vane.trifles.items.EmptyXpBottle$EmptyXpBottleVariant@3e3ea4ab with base material GLASS_BOTTLE and model_data
			// 7758382: org.oddlama.vane.trifles.items.XpBottle variant org.oddlama.vane.trifles.items.XpBottle$XpBottleVariant@109d018b with base material HONEY_BOTTLE and model_data
			// 7758383: org.oddlama.vane.trifles.items.XpBottle variant org.oddlama.vane.trifles.items.XpBottle$XpBottleVariant@46e696d with base material HONEY_BOTTLE and model_data
			// 7758384: org.oddlama.vane.trifles.items.XpBottle variant org.oddlama.vane.trifles.items.XpBottle$XpBottleVariant@126316e1 with base material HONEY_BOTTLE and model_data
			// 7758446: org.oddlama.vane.trifles.items.HomeScroll variant org.oddlama.vane.trifles.items.HomeScroll$HomeScrollVariant@7e2cc665 with base material CARROT_ON_A_STICK and model_data
			case 7758446: return get_module().item_registry().get(NamespacedKey.fromString("vane_trifles:home_scroll"));
			// 7758510: org.oddlama.vane.trifles.items.UnstableScroll variant org.oddlama.vane.trifles.items.UnstableScroll$UnstableScrollVariant@73a35ad4 with base material CARROT_ON_A_STICK and model_data
			// 7758574: org.oddlama.vane.trifles.items.ReinforcedElytra variant org.oddlama.vane.trifles.items.ReinforcedElytra$ReinforcedElytraVariant@8fccdba with base material ELYTRA and model_data
			// 7823790: org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge variant org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge$AncientTomeOfKnowledgeVariant@27a64533 with base material BOOK and model_data
			// 7823791: org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge variant org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge$AncientTomeOfKnowledgeVariant@605af50d with base material ENCHANTED_BOOK and model_data
			// 7823854: org.oddlama.vane.enchantments.items.AncientTomeOfTheGods variant org.oddlama.vane.enchantments.items.AncientTomeOfTheGods$AncientTomeOfTheGodsVariant@242cd56 with base material BOOK and model_data
			// 7823855: org.oddlama.vane.enchantments.items.AncientTomeOfTheGods variant org.oddlama.vane.enchantments.items.AncientTomeOfTheGods$AncientTomeOfTheGodsVariant@26bda869 with base material ENCHANTED_BOOK and model_data
		}

		return null;
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

				contents[i] = CustomItemHelper.convertExistingStack(convert_to_custom_item, is);
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
				contents[i] = CustomItemHelper.convertExistingStack(custom_item, is);
				get_module().log.info("Updated item " + custom_item.key());
				++changed;
				continue;
			}

			// Update maximum durability on existing items if changed.
			if (meta.getPersistentDataContainer().getOrDefault(DurabilityManager.ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, -1) != custom_item.durability()) {
				get_module().log.info("Updated item durability " + custom_item.key());
				DurabilityManager.initialize_or_update_max(custom_item, contents[i]);
				++changed;
				continue;
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
