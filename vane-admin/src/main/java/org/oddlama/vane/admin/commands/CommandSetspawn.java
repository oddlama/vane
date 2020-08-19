package org.oddlama.vane.admin.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.command.Description;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.command.Command;

// TODO
@Name("setspawn")
@Description("Yesss")
//@Usage("§")
public class CommandSetspawn extends Command {
	public CommandSetspawn(Module module) {
		super(module);
	}

	//@Override
	//public boolean onExecute(CommandSender sender, String alias, String[] args) {
	//	if (!(sender instanceof Player)) {
	//		sender.sendMessage(Configuration.MESSAGE_NOT_A_PLAYER.get());
	//		return true;
	//	}

	//	final Player player = (Player)sender;
	//	TableGeneric.set(TableGeneric.Key.Spawn, Convert.serializeLocation(player.getLocation()));
	//	player.sendMessage(Configuration.SETSPAWN_MESSAGE_SET.get());
	//	return true;
	//}
}
