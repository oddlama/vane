package org.oddlama.vane.core.command.check;

import java.util.ArrayList;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public class ErrorCheckResult implements CheckResult {
	private int depth;
	private String message;
	private String arg_chain = "";

	public ErrorCheckResult(int depth, String message) {
		this.depth = depth;
		this.message = message;
	}

	@Override
	public int depth() {
		return depth;
	}

	@Override
	public boolean good() {
		return false;
	}

	@Override
	public boolean apply(CommandSender sender) {
		return apply(sender, "");
	}

	public boolean apply(CommandSender sender, String indent) {
		var str = indent;
		if (indent == "") {
			str += "§cerror: ";
		}
		str += "§6";
		str += arg_chain;
		str += message;
		sender.sendMessage(str);
		return false;
	}

	@Override
	public CheckResult prepend(String argument_type, Object parsed_arg, boolean include) {
		// Save parsed arguments in argument chain, and propagate error
		arg_chain = "§3" + argument_type + "§6 → " + arg_chain;
		return this;
	}
}
