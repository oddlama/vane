package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Message;

@Name("vane")
public class Vane extends Command<Core> {
	@LangMessage
	private Message lang_reload_success;
	@LangMessage
	private Message lang_reload_fail;

	@LangString
	private String lang_resource_pack_create_success;

	public Vane(Context<Core> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);

		// Command parameters
		var reload = params().fixed("reload").ignore_case();
		reload.exec(this::reload_all);
		reload.choose_module().exec(this::reload_module);

		params().fixed("create_resource_pack").ignore_case().exec(this::create_resource_pack);
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

	private void create_resource_pack(CommandSender sender) {
		get_module().create_resource_pack();
		sender.sendMessage(lang_resource_pack_create_success);
	}
}
