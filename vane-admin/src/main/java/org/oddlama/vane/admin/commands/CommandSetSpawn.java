
package sublime.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sublime.Configuration;
import sublime.Privileges.Privilege;
import sublime.database.tables.TableGeneric;
import sublime.util.Convert;

public class CommandSetspawn extends SublimeCommand {
	public CommandSetspawn() {
		super("setspawn", new String[] {}, Configuration.SETSPAWN_MESSAGE_HELP, Configuration.SETSPAWN_MESSAGE_DESCRIPTION, null);
	}

	@Override
	public Privilege getRequiredPrivilege() {
		return Privilege.ADMIN;
	}

	@Override
	public boolean onExecute(CommandSender sender, String alias, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Configuration.MESSAGE_NOT_A_PLAYER.get());
			return true;
		}

		final Player player = (Player)sender;
		TableGeneric.set(TableGeneric.Key.Spawn, Convert.serializeLocation(player.getLocation()));
		player.sendMessage(Configuration.SETSPAWN_MESSAGE_SET.get());
		return true;
	}
}
