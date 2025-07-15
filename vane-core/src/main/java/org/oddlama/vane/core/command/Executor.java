package org.oddlama.vane.core.command;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface Executor {
    public boolean execute(Command<?> command, CommandSender sender, List<Object> parsed_args);
}
