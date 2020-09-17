package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.ItemUtil.ItemStackComparator;
import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.Arrays;

import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

public class ChestSorter extends Listener<Trifles> {
	private static final NamespacedKey LAST_SORT_TIME = namespaced_key("vane_trifles", "last_sort_time");

	@ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
	public long config_cooldown;

	public ChestSorter(Context<Trifles> context) {
		super(context.group("chest_sorting", "Enables chest sorting when a nearby button is pressed."));
	}

	private void sort_chest(final Block block) {
		final var state = block.getState();
		if (!(state instanceof Chest)) {
			return;
		}

		final var chest = (Chest)state;
		final var inventory = chest.getInventory();

		// Get persistent data
		final Chest persistent_chest;
		if (inventory instanceof DoubleChestInventory) {
			final var left_side = (((DoubleChestInventory)inventory).getLeftSide()).getHolder();
			if (!(left_side instanceof Chest)) {
				return;
			}
			persistent_chest = (Chest)left_side;
		} else {
			persistent_chest = chest;
		}

		// Check cooldown
		final var persistent_data = persistent_chest.getPersistentDataContainer();
		final var last_sort = persistent_data.getOrDefault(LAST_SORT_TIME, PersistentDataType.LONG, 0l);
		final var now = System.currentTimeMillis();
		if (now - last_sort < config_cooldown) {
			return;
		}

		persistent_data.set(LAST_SORT_TIME, PersistentDataType.LONG, now);
		if (persistent_chest != chest) {
			// Save left side block state if we are the right side
			persistent_chest.update(true, false);
		}

		// Find amount of non null item stacks
		final var saved_contents = inventory.getStorageContents();
		int non_null = 0;
		for (final var i : saved_contents) {
			if (i != null) {
				++non_null;
			}
		}

		// Make new array without null items
		final var saved_contents_condensed = new ItemStack[non_null];
		int cur = 0;
		for (final var i : saved_contents) {
			if (i != null) {
				saved_contents_condensed[cur++] = i.clone();
			}
		}

		// Clear and add all items again to stack them. Restore saved contents on failure.
		try {
			inventory.clear();
			final var leftovers = inventory.addItem(saved_contents_condensed);
			if (leftovers.size() != 0) {
				// Abort! Something went totally wrong!
				inventory.setStorageContents(saved_contents_condensed);
				get_module().log.warning("Sorting a chest produced leftovers!");
			}
		} catch (Exception e) {
			inventory.setStorageContents(saved_contents_condensed);
			throw e;
		}

		// Sort
		final var contents = inventory.getStorageContents();
		Arrays.sort(contents, new ItemStackComparator());
		inventory.setStorageContents(contents);
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
					if (block.getState() instanceof Chest) {
						sort_chest(block);
					}
				}
			}
		}
	}
}
