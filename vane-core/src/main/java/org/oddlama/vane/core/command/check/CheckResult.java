package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public interface CheckResult {
    public int depth();

    public boolean apply(Command<?> command, CommandSender sender);

    public CheckResult prepend(String argument_type, Object parsed_arg, boolean include);

    public boolean good();
}
