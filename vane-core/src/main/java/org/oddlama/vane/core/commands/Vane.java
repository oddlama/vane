package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.lang.TranslatedString;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;


@Name("vane")
public class Vane extends Command<Core> {
	@LangMessage
	private TranslatedMessage lang_reload_success;
	@LangMessage
	private TranslatedMessage lang_reload_fail;

	@LangString
	private TranslatedString lang_resource_pack_generate_success;
	@LangString
	private TranslatedString lang_resource_pack_generate_fail;

	private void test(final org.bukkit.entity.Player player) {
		org.oddlama.vane.core.menu.MenuFactory.item_chooser(get_module(), player, "TAITLE", new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL), true,
			(p, i) -> { p.sendMessage("" + i); },
			(p) -> { p.sendMessage("canzl"); }
		).open(player);
	}
	public Vane(Context<Core> context) {
		super(context);

		// Add help
		params().fixed("m").ignore_case().exec_player(this::test);
		params().fixed("help").ignore_case().exec(this::print_help);

		// Command parameters
		var reload = params().fixed("reload").ignore_case();
		reload.exec(this::reload_all);
		reload.choose_module().exec(this::reload_module);

		params().fixed("generate_resource_pack").ignore_case().exec(this::generate_resource_pack);
	}

	private void reload_module(CommandSender sender, Module<?> module) {
		if (module.reload_configuration()) {
			sender.sendMessage(lang_reload_success.format("vane-" + module.get_name()));
		} else {
			sender.sendMessage(lang_reload_fail.format("vane-" + module.get_name(), "invalid configuration"));
		}
	}

	private void reload_all(CommandSender sender) {
		for (var m : get_module().core.get_modules()) {
			reload_module(sender, m);
		}
	}

	private void generate_resource_pack(CommandSender sender) {
		if (get_module().generate_resource_pack()) {
			sender.sendMessage(lang_resource_pack_generate_success);
		} else {
			sender.sendMessage(lang_resource_pack_generate_fail);
		}
	}
}
