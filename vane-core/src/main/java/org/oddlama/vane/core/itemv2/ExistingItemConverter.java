package org.oddlama.vane.core.itemv2;

import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class ExistingItemConverter extends Listener<Core> {
	public ExistingItemConverter(final Context<Core> context) {
		super(context.namespace("existing_item_converter"));
	}

	private void process_inventory(@NotNull Inventory inventory) {
		final var contents = inventory.getContents();
		int changed = 0;

		for (int i = 0; i < contents.length; ++i) {
			final var is = contents[i];
			final var custom_item = get_module().item_registry().get(is);
			if (custom_item == null) {
				continue;
			}

			// Remove obsolete custom items
			if (get_module().item_registry().shouldRemove(custom_item.key())) {
				contents[i] = null;
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
				++changed;
				continue;
			}

			// Update maximum durability on existing items if changed.
			if (meta.getPersistentDataContainer().getOrDefault(DurabilityManager.ITEM_DURABILITY_MAX, PersistentDataType.INTEGER, -1) != custom_item.durability()) {
				DurabilityManager.initialize_or_update_max(custom_item, contents[i]);
			}
		}

		if (changed > 0) {
			inventory.setContents(contents);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_login(final PlayerLoginEvent event) {
		process_inventory(event.getPlayer().getInventory());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_chunk_load(final ChunkLoadEvent event) {
		final var chunk = event.getChunk();
		for (final var te : chunk.getTileEntities()) {
			if (te instanceof Container container) {
				process_inventory(container.getInventory());
			}
		}
	}
}
