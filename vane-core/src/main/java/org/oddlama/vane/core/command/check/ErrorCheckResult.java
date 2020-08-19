package org.oddlama.vane.core.command.check;

import java.util.Optional;
import org.bukkit.command.CommandSender;

public class ErrorCheckResult implements CheckResult {
	private String message;

	public ErrorCheckResult(String message) {
		this.message = message;
	}

	@Override
	public boolean good() {
		return false;
	}

	@Override
	public boolean apply(CommandSender sender) {
		sender.sendMessage("§cerror:§6 " + message);
		return apply(sender, 0);
	}

	public boolean apply(CommandSender sender, int depth) {
		var indent = "";
		for (int i = 0; i < depth; ++i) {
			indent += "  ";
		}
		sender.sendMessage(indent + "- " + message);
		return false;
	}

	@Override
	public CheckResult prepend(Object parsed_arg) {
		// Throw away parsed arguments, propagate error instead
		return this;
	}
}
