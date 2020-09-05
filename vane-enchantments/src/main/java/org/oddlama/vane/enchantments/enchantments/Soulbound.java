package org.oddlama.vane.enchantments.enchantments;

import java.util.List;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allow_custom = true)
public class Soulbound extends CustomEnchantment<Enchantments> {
	public Soulbound(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void apply_display_format(BaseComponent component) {
		component.setColor(ChatColor.DARK_GRAY);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_death(final PlayerDeathEvent event) {
		final var keep_items = event.getItemsToKeep();

		// Keep all soulbound items
		final var it = event.getDrops().iterator();
		while (it.hasNext()) {
			final var drop = it.next();
			if (drop.getEnchantmentLevel(this.bukkit()) > 0) {
				keep_items.add(drop);
				it.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_drop_item(final PlayerDropItemEvent event) {
		// Soulbound items cannot be dropped by a player.
		// Prevents yeeting your best sword out of existence.
		// (It's okay to put them into chests)
		final var dropped_item = event.getItemDrop().getItemStack();
		if (dropped_item.getEnchantmentLevel(this.bukkit()) > 0) {
			final var inventory = event.getPlayer().getInventory();
			if (inventory.firstEmpty() != -1) {
				// We still have space in the inventory, so the player tried to drop it with Q.
				event.setCancelled(true);
			} else {
				// Inventory is full (e.g. when exiting crafting table with soulbound item in it)
				// so we drop the first non-soulbound item (if any) instead.
				final var it = inventory.iterator();
				ItemStack non_soulbound_item = null;
				int non_soulbound_item_slot = 0;
				while (it.hasNext()) {
					final var item = it.next();
					if (item.getEnchantmentLevel(this.bukkit()) == 0) {
						non_soulbound_item = item;
						break;
					}

					++non_soulbound_item_slot;
				}

				if (non_soulbound_item == null) {
					// We can't prevent dropping a soulbound item.
					// Well that sucks.
					return;
				}

				// Drop the other item
				final var player = event.getPlayer();
				inventory.setItem(non_soulbound_item_slot, dropped_item);
				player.getLocation().getWorld().dropItem(player.getLocation(), non_soulbound_item);
				event.setCancelled(true);
			}
		}
	}
}
