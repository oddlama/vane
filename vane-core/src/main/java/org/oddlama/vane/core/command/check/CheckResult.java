package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

public interface CheckResult {
	public boolean apply(CommandSender sender);
	public CheckResult prepend(Object parsed_arg);
	public boolean good();
}
