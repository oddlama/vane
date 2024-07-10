package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.ItemUtil.ItemStackComparator;

import java.util.Arrays;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.data.CooldownData;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.StorageUtil;

public class ChestSorter extends Listener<Trifles> {

	public static final NamespacedKey LAST_SORT_TIME = StorageUtil.namespaced_key("vane_trifles", "last_sort_time");

	@ConfigLong(def = 1000, min = 0, desc = "Chest sorting cooldown in milliseconds.")
	public long config_cooldown;

	private CooldownData cooldown_data = null;

	@ConfigInt(
		def = 1,
		min = 0,
		max = 16,
		desc = "Chest sorting radius in X-direction from the button (left-right when looking at the button). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
	)
	public int config_radius_x;

	@ConfigInt(
		def = 1,
		min = 0,
		max = 16,
		desc = "Chest sorting radius in Y-direction from the button (up-down when looking at the button - this can be a horizontal direction if the button is on the ground). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
	)
	public int config_radius_y;

	@ConfigInt(
		def = 1,
		min = 0,
		max = 16,
		desc = "Chest sorting radius in Z-direction from the button (into/out-of the attached block). A radius of 0 means a column of the block including the button. It is advised to NEVER set the three radius values to more than THREE (3), as sorting a huge area of chests can lead to SEVERE lag! Ideally always keep the Z-radius set to 0 or 1, while only adjusting X and Y. You've been warned."
	)
	public int config_radius_z;

	public ChestSorter(Context<Trifles> context) {
		super(context.group("chest_sorting", "Enables chest sorting when a nearby button is pressed."));
	}

	@Override
	protected void on_config_change() {
		super.on_config_change();
		this.cooldown_data = new CooldownData(LAST_SORT_TIME, config_cooldown);
	}

	private void sort_inventory(final Inventory inventory) {
		// Find number of non-null item stacks
		final var saved_contents = inventory.getStorageContents();
		int non_null = 0;
		for (final var i : saved_contents) {
			if (i != null) {
				++non_null;
			}
		}

		// Make a new array without null items
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
				get_module().log.warning("Sorting inventory " + inventory + " produced leftovers!");
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

	private void sort_container(final Container container) {
		// Check cooldown
		if (!cooldown_data.check_or_update_cooldown(container)) {
			return;
		}

		sort_inventory(container.getInventory());
	}

	private void sort_chest(final Chest chest) {
		final var inventory = chest.getInventory();

		// Get persistent data
		final Chest persistent_chest;
		if (inventory instanceof DoubleChestInventory) {
			final var left_side = (((DoubleChestInventory) inventory).getLeftSide()).getHolder();
			if (!(left_side instanceof Chest)) {
				return;
			}
			persistent_chest = (Chest) left_side;
		} else {
			persistent_chest = chest;
		}

		// Check cooldown
		if (!cooldown_data.check_or_update_cooldown(persistent_chest)) {
			return;
		}

		if (persistent_chest != chest) {
			// Save the left side block state if we are the right side
			persistent_chest.update(true, false);
		}

		sort_inventory(inventory);
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

		final var button_data = root_block.getState().getBlockData();
		final var facing = ((Directional) button_data).getFacing();
		final var face = ((FaceAttachable) button_data).getAttachedFace();

		int rx = 0;
		int ry;
		int rz = 0;

		// Determine relative radius rx, ry, rz as seen from the button.
		if (face == FaceAttachable.AttachedFace.WALL) {
			ry = config_radius_y;
			switch (facing) {
				case NORTH:
				case SOUTH:
					rx = config_radius_x;
					rz = config_radius_z;
					break;
				case EAST:
				case WEST:
					rx = config_radius_z;
					rz = config_radius_x;
					break;
				default:
					break;
			}
		} else {
			ry = config_radius_z;
			switch (facing) {
				case NORTH:
				case SOUTH:
					rx = config_radius_x;
					rz = config_radius_y;
					break;
				case EAST:
				case WEST:
					rx = config_radius_y;
					rz = config_radius_x;
					break;
				default:
					break;
			}
		}

		// Find chests in configured radius and sort them.
		for (int x = -rx; x <= rx; ++x) {
			for (int y = -ry; y <= ry; ++y) {
				for (int z = -rz; z <= rz; ++z) {
					final var block = root_block.getRelative(x, y, z);
					final var state = block.getState();
					if (state instanceof Chest) {
						sort_chest((Chest) state);
					} else if (state instanceof Barrel) {
						sort_container((Barrel) state);
					}
				}
			}
		}
	}
}
