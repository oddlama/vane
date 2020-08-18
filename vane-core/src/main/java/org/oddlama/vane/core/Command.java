package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;

import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Description;

@VaneCommand
public abstract class Command extends org.bukkit.command.Command implements PluginIdentifiableCommand {
	//public List<String> identifiers = new ArrayList<>();
	//private TabCompletion root = null;
	Module module;

	public Command(Module module) {
		super("");
		this.module = module;

		// Load annotation values
		//if (!getClass().isAnnotationPresent(Name.class)) {
		//	throw new RuntimeException("Command '" + getClass().getName() + "' is missing the @Name annotation");
		//}
		var name = getClass().getAnnotation(Name.class).value();
		setLabel(name);
		setName(name);

		var desc = getClass().getAnnotation(Description.class).value();
		setDescription(desc);

		var aliases = getClass().getAnnotation(Aliases.class);
		if (aliases != null) {
			setAliases(List.of(aliases.value()));
		}

		//final List<String> list = Arrays.asList(aliases);
		//setAliases(list);

		//this.setDescription(desciption);

		//root = completion;

		//identifiers.add(name);
		//identifiers.addAll(list);
	}

	public String get_name() {
		return getName();
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	@Override
	public Plugin getPlugin() {
		return module;
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args) {
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		return null;
	}
}
