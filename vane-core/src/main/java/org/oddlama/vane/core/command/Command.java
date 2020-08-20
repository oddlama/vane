package org.oddlama.vane.core.command;

import static org.oddlama.vane.util.Util.prepend;

import java.util.List;
import java.util.Collections;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.lang.LangStringField;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.annotation.lang.LangString;

@VaneCommand
public abstract class Command extends org.bukkit.command.Command implements PluginIdentifiableCommand {
	private AnyParam<String> root_param;
	protected Module module;

	@LangString
	public String lang_usage;

	@LangString
	public String lang_description;

	public Command(Module module) {
		super("");
		this.module = module;

		// Load annotation values
		var name = getClass().getAnnotation(Name.class).value();
		setLabel(name);
		setName(name);

		var aliases = getClass().getAnnotation(Aliases.class);
		if (aliases != null) {
			setAliases(List.of(aliases.value()));
		}

		// Add localization fields to language manager
		module.lang_manager.compile(this, s -> "command_" + name + "_" + s);

		// Initialize root parameter
		root_param = new AnyParam<String>(this, "/" + getName(), str -> str);
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	public Param params() {
		return root_param;
	}

	public void print_help(CommandSender sender) {
		sender.sendMessage("§7/§3" + getName() + " " + getUsage());
		sender.sendMessage(getDescription());
	}

	@Override
	public String getPermission() {
		return "vane." + module.get_name() + ".commands." + getName();
	}

	@Override
	public String getUsage() {
		return lang_usage;
	}

	@Override
	public String getDescription() {
		return lang_description;
	}

	@Override
	public Plugin getPlugin() {
		return module;
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args) {
		// Ambigous matches will always execute the
		// first chain based on definition order.
		try {
			return root_param.check_accept(prepend(args, alias), 0).apply(this, sender);
		} catch (Exception e) {
			sender.sendMessage("§cAn unexpected error occurred. Please examine the console log and/or notify a server administator.");
			throw e;
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		// Don't allow information exfiltration!
		if (!sender.hasPermission(getPermission())) {
			return Collections.emptyList();
		}

		try {
			return root_param.build_completions(prepend(args, alias), 0);
		} catch (Exception e) {
			sender.sendMessage("§cAn unexpected error occurred. Please examine the console log and/or notify a server administator.");
			throw e;
		}
	}
}
