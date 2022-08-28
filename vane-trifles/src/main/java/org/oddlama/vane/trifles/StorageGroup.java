package org.oddlama.vane.trifles;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Nameable;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

import net.kyori.adventure.text.Component;

public class StorageGroup extends Listener<Trifles> {
	private Map<Inventory, Pair<UUID, ItemStack>> open_block_state_inventories = Collections
			.synchronizedMap(new HashMap<Inventory, Pair<UUID, ItemStack>>());

	@LangMessage
	public TranslatedMessage lang_open_stacked_item;

	public StorageGroup(Context<Trifles> context) {
		super(context.group("storage", "Extensions to storage related stuff will be grouped under here."));
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_place_item_in_storage_inventory(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		// Only if no block state inventory is open, else we could delete items by accident
		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item != null) {
			return;
		}

		// Put non-storage items in a right-clicked storage item
		if (event.getClick() == ClickType.RIGHT && event.getAction() == InventoryAction.SWAP_WITH_CURSOR
				&& is_storage_item(event.getCurrentItem())) {

			// Allow putting in any items that are not a storage item, or storage items that have nothing in them.
			if (!(is_storage_item(event.getCursor()) && event.getCursor().hasItemMeta())) {
				final var custom_item = get_module().core.item_registry().get(event.getCurrentItem());

				// Only if the clicked storage item is a custom item
				if (custom_item != null) {
					event.getCurrentItem().editMeta(BlockStateMeta.class, meta -> {
						final var block_state = meta.getBlockState();
						if (block_state instanceof Container container) {
							final var leftovers = container.getInventory().addItem(event.getCursor());
							if (leftovers.size() == 0) {
								event.setCursor(null);
							} else {
								event.setCursor(leftovers.get(0));
							}
							meta.setBlockState(block_state);
						}
					});
				}
			}

			// right-clicking a storage item to swap is never "allowed".
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_inventory_click(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item == null || !owner_and_item.getLeft().equals(player.getUniqueId())) {
			return;
		}

		// Prevent putting non-empty storage items in other storage items
		if (is_storage_item(event.getCurrentItem()) || (is_storage_item(event.getCursor()) && event.getCursor().hasItemMeta())) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_inventory_drag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item == null || !owner_and_item.getLeft().equals(player.getUniqueId())) {
			return;
		}

		// Prevent putting storage items in other storage items
		for (final var item_stack : event.getNewItems().values()) {
			if (is_storage_item(item_stack)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void save_after_click(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item == null || !owner_and_item.getLeft().equals(player.getUniqueId())) {
			return;
		}

		update_storage_item(owner_and_item.getRight(), event.getInventory());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void save_after_drag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item == null || !owner_and_item.getLeft().equals(player.getUniqueId())) {
			return;
		}

		update_storage_item(owner_and_item.getRight(), event.getInventory());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void save_after_close(InventoryCloseEvent event) {
		final var owner_and_item = open_block_state_inventories.get(event.getInventory());
		if (owner_and_item == null || !owner_and_item.getLeft().equals(event.getPlayer().getUniqueId())) {
			return;
		}

		update_storage_item(owner_and_item.getRight(), event.getInventory());
		open_block_state_inventories.remove(event.getInventory());
	}

	private boolean is_storage_item(@Nullable ItemStack item) {
		// Any item that has a container block state as the meta is a container to us.
		// If the item has no meta (i.e. is empty) it doesnt count.
		return item != null
				&& item.getItemMeta() instanceof BlockStateMeta meta
				&& meta.getBlockState() instanceof Container;
	}

	private void update_storage_item(@NotNull ItemStack item, @NotNull Inventory inventory) {
		item.editMeta(BlockStateMeta.class, meta -> {
			final var block_state = meta.getBlockState();
			if (block_state instanceof Container container) {
				container.getInventory().setContents(inventory.getContents());
				meta.setBlockState(block_state);
			}
		});
	}

	public boolean open_block_state_inventory(@NotNull final Player player, @NotNull ItemStack item) {
		// Require correct block state meta
		if (!(item.getItemMeta() instanceof BlockStateMeta meta)
				|| !(meta.getBlockState() instanceof Container container)) {
			return false;
		}

		// Only if the stack size is 1.
		if (item.getAmount() != 1) {
			get_module().storage_group.lang_open_stacked_item.send_action_bar(player);
			return false;
		}

		// Transfer item name to block-state
		Component name = null;
		if (meta.getBlockState() instanceof Nameable nameable) {
			name = meta.hasDisplayName() ? meta.displayName() : null;
			nameable.customName(name);
		}

		// Create transient inventory
		final var transient_inventory = get_module().getServer().createInventory(player,
				container.getInventory().getType(), name);
		transient_inventory.setContents(container.getInventory().getContents());

		// Open inventory
		open_block_state_inventories.put(transient_inventory, Pair.of(player.getUniqueId(), item));
		player.openInventory(transient_inventory);
		return true;
	}
}
