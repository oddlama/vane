package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.command.Name;
import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Message;

@VaneEnchantment(name = "rake", max_level = 4)
public class Rake extends CustomEnchantment<Enchantments> {
	public Rake(Context<Enchantments> context) {
		super(context);
	}
}
