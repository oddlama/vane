package org.oddlama.vane.core.command.check;

import java.util.List;
import java.util.Collections;
import org.bukkit.command.CommandSender;

public class CombinedErrorCheckResult extends ErrorCheckResult {
	private List<ErrorCheckResult> errors;

	public CombinedErrorCheckResult(List<ErrorCheckResult> errors) {
		super(errors.get(0).depth(), "§6could not match one of:§r");
		if (errors.size() < 2) {
			throw new RuntimeException("Tried to create CombinedErrorCheckResult with less than 2 sub-errors! This is a bug.");
		}
		this.errors = errors;
	}

	@Override
	public boolean apply(CommandSender sender, String indent) {
		//if (errors.size() == 1) {
		//	errors.get(0).apply(sender, depth);
		//} else {
			super.apply(sender, indent);
			for (var err : errors) {
				err.apply(sender, indent + "  ");
			}
		//}
		return false;
	}
}
