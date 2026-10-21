package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

/**
 * Error result aggregating multiple branch failures at the same parse depth.
 *
 * @param errors collected sub-errors.
 */
class CombinedErrorCheckResult(private val errors: List<ErrorCheckResult>) :
    ErrorCheckResult(errors.first().depth(), "§6could not match one of:§r") {

    init {
        require(errors.size >= 2) {
            "Tried to create CombinedErrorCheckResult with less than 2 sub-errors! This is a bug."
        }
    }

    /** Sends the group header and each nested sub-error. */
    override fun apply(command: Command<*>?, sender: CommandSender?, indent: String): Boolean {
        super.apply(command, sender, indent)
        errors.forEach { it.apply(command, sender, "$indent  ") }
        return false
    }
}
