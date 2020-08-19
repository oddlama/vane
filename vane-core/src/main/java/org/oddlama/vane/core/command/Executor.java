package org.oddlama.vane.core.command;

import java.util.List;

import org.bukkit.command.CommandSender;

public interface Executor {
	public boolean execute(CommandSender sender, List<Object> parsed_args);
}
