package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;

public abstract class Command extends org.bukkit.command.Command implements PluginIdentifiableCommand {
	//public List<String> identifiers = new ArrayList<>();
	//private TabCompletion root = null;

	public Command(String name, String[] aliases, String desciption) {
		super(name);

		//final List<String> list = Arrays.asList(aliases);
		//setAliases(list);

		//this.setDescription(desciption);

		//root = completion;

		//identifiers.add(name);
		//identifiers.addAll(list);
	}

	//public abstract Privilege getRequiredPrivilege();
	public abstract boolean onExecute(CommandSender sender, String alias, String[] args);

	//public boolean canExecute(CommandSender sender) {
	//	return Privileges.hasPrivilege(sender, getRequiredPrivilege());
	//}

	//public boolean matchesStart(String id) {
	//	for (String i : identifiers) {
	//		if (i.startsWith(id))
	//			return true;
	//	}

	//	return false;
	//}

	//public boolean matches(String id) {
	//	return identifiers.contains(id);
	//}

	@Override
	public Plugin getPlugin() {
		return null;
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args) {
		return true;
		//if (sender == null || alias == null || args == null)
		//	return true;

		//if (!Global.main.isEnabled() || !canExecute(sender))
		//	return true;

		//return onExecute(sender, alias, args);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws CommandException, IllegalArgumentException {
		return null;
		//if (sender == null || alias == null || args == null)
		//	return null;

		//if (root == null || !Global.main.isEnabled() || !canExecute(sender))
		//	return null;

		//TabCompletion completion = root;
		//for (int index = 0; index < args.length - 1; index++) {
		//	completion = completion.getCompletion(sender, args[index]);
		//	if (completion == null)
		//		return null; /* no completion up to this path */
		//}

		//return completion.complete(sender, args, args.length - 1);
	}
}
