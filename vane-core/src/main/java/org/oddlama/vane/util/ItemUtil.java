package org.oddlama.vane.util;

import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.player_handle;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {
	public static void damage_item(final Player player, ItemStack item_stack, int amount) {
		if (amount <= 0) {
			return;
		}

		final var handle = item_handle(item_stack);
		if (handle == null) {
			return;
		}

		handle.damage(amount, player_handle(player), x -> {});
	}
}
