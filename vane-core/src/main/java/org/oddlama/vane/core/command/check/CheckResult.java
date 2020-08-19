package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

public interface CheckResult {
	public int depth();
	public boolean apply(CommandSender sender);
	public CheckResult prepend(String argument_type, Object parsed_arg, boolean include);
	public boolean good();
}
