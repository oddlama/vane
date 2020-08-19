package org.oddlama.vane.core.command.check;

import java.util.List;
import java.util.Collections;
import org.bukkit.command.CommandSender;

public class CombinedErrorCheckResult extends ErrorCheckResult {
	private List<ErrorCheckResult> errors;

	public CombinedErrorCheckResult(List<ErrorCheckResult> errors) {
		super("could not match subparameters:");
		this.errors = errors;
	}

	@Override
	public boolean apply(CommandSender sender, int depth) {
		super.apply(sender, depth);
		for (var err : errors) {
			err.apply(sender, depth + 1);
		}
		return false;
	}
}
