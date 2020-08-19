package org.oddlama.vane.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.Plugin;
import static org.oddlama.vane.util.Util.prepend;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;

import org.oddlama.vane.core.Module;
import org.oddlama.vane.annotation.command.VaneCommand;
import java.util.Collection;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Description;

@VaneCommand
public abstract class Command extends org.bukkit.command.Command implements Param, PluginIdentifiableCommand {
	protected Module module;

	public Command(Module module) {
		super("");
		this.module = module;

		// Load annotation values
		var name = getClass().getAnnotation(Name.class).value();
		setLabel(name);
		setName(name);

		var desc = getClass().getAnnotation(Description.class).value();
		setDescription(desc);

		var aliases = getClass().getAnnotation(Aliases.class);
		if (aliases != null) {
			setAliases(List.of(aliases.value()));
		}
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	@Override
	public Plugin getPlugin() {
		return module;
	}

	@Override
	public Command get_command() {
		return this;
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args) {
		return check_accept(args, -1).apply(sender);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		return null;
	}
}
