package org.oddlama.vane.core.command;

import static org.oddlama.vane.util.Util.prepend;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.command.params.AnyParam;

@VaneCommand
public abstract class Command<T> extends ModuleComponent<T> {
	private class BukkitCommand extends org.bukkit.command.Command implements PluginIdentifiableCommand {
		public BukkitCommand() {
			super("");
		}

		@Override
		public String getPermission() {
			return "vane." + Command.this.get_module().get_name() + ".commands." + getName();
		}

		@Override
		public String getUsage() {
			return Command.this.lang_usage;
		}

		@Override
		public String getDescription() {
			return Command.this.lang_description;
		}

		@Override
		public Plugin getPlugin() {
			return Command.this.get_module();
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

	// Language
	@LangString
	public String lang_usage;

	@LangString
	public String lang_description;

	// Root parameter
	private String name;
	private BukkitCommand bukkit_command;
	private AnyParam<String> root_param;

	public Command(Context<T> context) {
		super(context.namespace("command_" + Command.class.getAnnotation(Name.class).value()));

		// Load annotation values
		name = Command.class.getAnnotation(Name.class).value();
		bukkit_command = new BukkitCommand(name);
		bukkit_command.setLabel(name);
		bukkit_command.setName(name);

		var aliases = Command.class.getAnnotation(Aliases.class);
		if (aliases != null) {
			bukkit_command.setAliases(List.of(aliases.value()));
		}

		// Initialize root parameter
		root_param = new AnyParam<String>(this, "/" + getName(), str -> str);
	}

	public BukkitCommand get_bukkit_command() {
		return bukkit_command;
	}

	public String get_name() {
		return name;
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	public Param params() {
		return root_param;
	}

	public void print_help(CommandSender sender) {
		sender.sendMessage("§7/§3" + bukkit_command.getName() + " " + bukkit_command.getUsage());
		sender.sendMessage(bukkit_command.getDescription());
	}
}
