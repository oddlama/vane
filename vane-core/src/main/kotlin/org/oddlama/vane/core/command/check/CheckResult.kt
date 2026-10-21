package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

/** Represents the outcome of parsing or matching a command argument node. */
interface CheckResult {
    /** Returns the parse depth where this result was produced. */
    fun depth(): Int

    /** Applies this result, usually by executing or reporting an error. */
    fun apply(command: Command<*>?, sender: CommandSender?): Boolean

    /** Prepends context from an argument parse step. */
    fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult

    /** Returns whether this is a successful result. */
    fun good(): Boolean
}
