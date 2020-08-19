package org.oddlama.vane.core.command.check;

import java.util.List;
import java.util.Collections;
import org.bukkit.command.CommandSender;

public class CombinedErrorCheckResult extends ErrorCheckResult {
	private List<ErrorCheckResult> errors;

	public CombinedErrorCheckResult(List<ErrorCheckResult> errors) {
		super(errors.get(0).depth(), "§6could not match subparameters:§r");
		this.errors = errors;
	}

	@Override
	public boolean apply(CommandSender sender, int depth, int chain_begin, String indent) {
		//if (errors.size() == 1) {
		//	errors.get(0).apply(sender, depth);
		//} else {
			super.apply(sender, depth, chain_begin, indent);
			for (var err : errors) {
				err.apply(sender, depth + 1, depth + 1, indent + "  ");
			}
		//}
		return false;
	}
}
