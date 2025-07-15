package org.oddlama.vane.core.command.check;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public class CombinedErrorCheckResult extends ErrorCheckResult {

    private List<ErrorCheckResult> errors;

    public CombinedErrorCheckResult(List<ErrorCheckResult> errors) {
        super(errors.get(0).depth(), "§6could not match one of:§r");
        if (errors.size() < 2) {
            throw new RuntimeException(
                "Tried to create CombinedErrorCheckResult with less than 2 sub-errors! This is a bug."
            );
        }
        this.errors = errors;
    }

    @Override
    public boolean apply(Command<?> command, CommandSender sender, String indent) {
        super.apply(command, sender, indent);
        for (var err : errors) {
            err.apply(command, sender, indent + "  ");
        }
        return false;
    }
}
