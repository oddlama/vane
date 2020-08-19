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
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.annotation.command.VaneCommand;
import java.util.Collection;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Description;

@VaneCommand
public abstract class Command extends org.bukkit.command.Command implements PluginIdentifiableCommand {
	private AnyParam<String> root_param;
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

		// Initialize root parameter
		root_param = new AnyParam<String>(this, "/" + getName(), str -> str);
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	public Param params() {
		return root_param;
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
			return root_param.check_accept(prepend(args, alias), 0).apply(sender);
		} catch (Exception e) {
			sender.sendMessage("§cAn unexpected error occurred. Please examine the console log and/or notify a server administator.");
			throw e;
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		return null;
	}
}
