package org.oddlama.vane.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.Plugin;
import java.util.function.Function;
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
	//private List<String> identifiers;

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

		// Collect all identifiers
		//identifiers = new ArrayList<>();
		//identifiers.add(name);
		//identifiers.addAll(aliases.value());
	}

	public String get_prefix() {
		return "vane:" + module.get_name();
	}

	@Override
	public Plugin getPlugin() {
		return module;
	}

	//protected<T extends String> Param any_param() {
	//	return root_param.<T>any_param();
	//}

	//protected<T> Param any_param(Function<T, String> to_string) {
	//	return root_param.<T>any_param(to_string);
	//}

	//protected Param fixed_param(String arg) {
	//	return root_param.fixed_param(arg);
	//}

	//protected<T> Param fixed_param(T arg, Function<T, String> to_string) {
	//	return root_param.fixed_param(arg, to_string);
	//}

	//protected Param choice_param(Collection<String> choices) {
	//	return root_param.choice_param(choices);
	//}

	//protected<T> Param choice_param(Collection<T> choices, Function<T, String> to_string) {
	//	return root_param.choice_param(choices, to_string);
	//}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args) {
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		return null;
	}
}
