package org.oddlama.vane.core.command.check;

import java.util.ArrayList;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public class ErrorCheckResult implements CheckResult {
	private int depth;
	private String message;
	private ArrayList<String> parsed_arg_types = new ArrayList<>();

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
		return apply(sender, 0, 0, "");
	}

	public boolean apply(CommandSender sender, int depth, int chain_begin, String indent) {
		var str = indent;
		if (chain_begin == 0) {
			str += "§cerror: ";
		}
		str += "§6";
		for (int i = 0; i < parsed_arg_types.size(); ++i) {
			str += parsed_arg_types.get(i) + " →";
		}
		sender.sendMessage(str + " " + message);
		return false;
	}

	@Override
	public CheckResult prepend(String argument_type, Object parsed_arg) {
		// Throw away parsed arguments, propagate error instead
		parsed_arg_types.add(0, argument_type);
		return this;
	}
}
