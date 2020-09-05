package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.ItemUtil.ItemStackComparator;

import java.util.Arrays;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.Event;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

import static org.oddlama.vane.util.Util.namespaced_key;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class ChestSorter extends Listener<Trifles> {
	private static final NamespacedKey LAST_SORT_TIME = namespaced_key("vane", "last_sort_time");

	@ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
	public long config_cooldown;

	public ChestSorter(Context<Trifles> context) {
		super(context.group("chest_sorting", "Enables chest sorting when a nearby button is pressed."));
	}

	private void sort_chest(final Block block) {
		final var state = block.getState(false);
		if (!(state instanceof Chest)) {
			return;
		}

		final var chest = (Chest)state;
		final var inventory = chest.getInventory();

		// Get persistent data
		final PersistentDataContainer persistent_data;
		if (state instanceof DoubleChest) {
			final var double_chest = (DoubleChest)state;
			final var left_side = double_chest.getLeftSide();
			if (!(left_side instanceof Chest)) {
				return;
			}
			persistent_data = ((Chest)left_side).getPersistentDataContainer();
		} else {
			persistent_data = chest.getPersistentDataContainer();
		}

		// Check cooldown
		final var last_sort = persistent_data.getOrDefault(LAST_SORT_TIME, PersistentDataType.LONG, 0l);
		final var now = System.currentTimeMillis();
		if (now - last_sort < config_cooldown) {
			return;
		}

		persistent_data.set(LAST_SORT_TIME, PersistentDataType.LONG, now);
		final var content = inventory.getStorageContents();
		// Stack items
		//for (int index = content.length - 1; index >= 0; --index) {
		//	items.set(index, sortAddItem(items, items.get(index), index));
		//}
		// Sort
		Arrays.sort(content, new ItemStackComparator());
		inventory.setStorageContents(content);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Require the action to be block usage
		if (event.useInteractedBlock() != Event.Result.ALLOW) {
			return;
		}

		// Require the clicked block to be a button
		final var root_block = event.getClickedBlock();
		if (!Tag.BUTTONS.isTagged(root_block.getType())) {
			return;
		}

		// Find nearby (3x3x3) chests and sort them.
		final var radius = 1;
		for (int x = -radius; x <= radius; ++x) {
			for (int y = -radius; y <= radius; ++y) {
				for (int z = -radius; z <= radius; ++z) {
					final var block = root_block.getRelative(x, y, z);
					if (block.getState(false) instanceof Chest) {
						sort_chest(block);
					}
				}
			}
		}
	}
}
