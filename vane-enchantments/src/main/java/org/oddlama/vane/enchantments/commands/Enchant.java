package org.oddlama.vane.enchantments.commands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.util.Message;

@Name("enchant")
public class Enchant extends Command<Enchantments> {
	@LangMessage
	private Message lang_level_too_low;
	@LangMessage
	private Message lang_level_too_high;
	@LangMessage
	private Message lang_invalid_enchantment;

	public Enchant(Context<Enchantments> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		var enchantment = params().choose_enchantment(this::filter_by_held_item);
		enchantment.exec_player(this::enchant_current_item_level_1);
		enchantment.any("level", str -> {
			try {
				return Integer.parseUnsignedInt(str);
			} catch (NumberFormatException e) {
				return null;
			}
		}).exec_player(this::enchant_current_item);
	}

	private boolean filter_by_held_item(CommandSender sender, Enchantment e) {
		if (!(sender instanceof Player)) {
			return false;
		}

		final var player = (Player)sender;
		final var item_stack = player.getEquipment().getItemInMainHand();
		boolean is_book = item_stack.getType() == Material.BOOK || item_stack.getType() == Material.ENCHANTED_BOOK;
		return is_book || e.canEnchantItem(item_stack);
	}

	private void enchant_current_item_level_1(Player player, Enchantment enchantment) {
		enchant_current_item(player, enchantment, 1);
	}

	private void enchant_current_item(Player player, Enchantment enchantment, Integer level) {
		if (level < enchantment.getStartLevel()) {
			player.sendMessage(lang_level_too_low.format(level, enchantment.getStartLevel()));
			return;
		} else if (level > enchantment.getMaxLevel()) {
			player.sendMessage(lang_level_too_high.format(level, enchantment.getMaxLevel()));
			return;
		}

		final var item_stack = player.getEquipment().getItemInMainHand();
		if (item_stack.getType() == Material.AIR) {
			player.sendMessage(lang_invalid_enchantment.format(enchantment.getKey().toString(), item_stack.getType().getKey().toString()));
			return;
		}

		try {
			// Convert book if necessary
			if (item_stack.getType() == Material.BOOK) {
				item_stack.setType(Material.ENCHANTED_BOOK); /* fallthrough */
			}

			if (item_stack.getType() == Material.ENCHANTED_BOOK) {
				final var meta = (EnchantmentStorageMeta)item_stack.getItemMeta();
				meta.addStoredEnchant(enchantment, level, false);
				item_stack.setItemMeta(meta);
			} else {
				item_stack.addEnchantment(enchantment, level);
			}

			get_module().update_enchanted_item(item_stack);
		} catch (Exception e) {
			player.sendMessage(lang_invalid_enchantment.format(enchantment.getKey().toString(), item_stack.getType().getKey().toString()));
			return;
		}
	}
}
