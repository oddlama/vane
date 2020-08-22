package org.oddlama.vane.admin.commands;

import java.util.List;
import static org.oddlama.vane.util.WorldUtil.change_time_smoothly;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.command.CommandSender;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.command.Command;

@Name("enchant")
public class Enchant extends Command<Admin> {
	public Enchant(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().choose_enchantment(this::filter_by_held_item).any_string().exec_player(this::enchant_current_item);
	}

	private boolean filter_by_held_item(CommandSender sender, Enchantment e) {
		if (!(sender instanceof Player)) {
			return false;
		}

		final var player = (Player)sender;
		final var item = player.getEquipment().getItemInMainHand();
		boolean is_book = item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;
		return is_book || e.canEnchantItem(item);
	}

	private void enchant_current_item(Player player, Enchantment enchantment, String level) {
		player.sendMessage("e: " + enchantment + " " + level);
		for (var w : get_module().getServer().getWorlds()) {
			player.sendMessage(w.getName());
		}
		//player.getEquipment().getItemInMainHand().;
	}
}
