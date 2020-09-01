package org.oddlama.vane.util;

import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.player_handle;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class ItemUtil {
	public static final UUID MODIFIER_UUID_GENERIC_ATTACK_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
	public static final UUID MODIFIER_UUID_GENERIC_ATTACK_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

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
