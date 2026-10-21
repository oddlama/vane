package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

/**
 * Successful parse result for a single parameter node.
 *
 * @param depth parse depth where this result was produced.
 * @param argumentType parsed argument type label.
 * @param parsed parsed argument value.
 * @param includeParam whether this parsed value should be forwarded to executors.
 */
class ParseCheckResult(
    /** Parse depth where this result was produced. */
    private val depth: Int,
    /** Parsed argument type label. */
    val argumentType: String,
    /** Parsed argument value. */
    val parsed: Any?,
    /** Whether parsed value should be included for execution arguments. */
    val includeParam: Boolean,
) : CheckResult {
    /** Returns the parse depth for this result. */
    override fun depth(): Int = depth

    /** Parse results are always successful. */
    override fun good(): Boolean = true

    /** Parse-only results cannot be directly applied. */
    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        error("ParseCheckResult cannot be applied! This is a bug.")

    /** Parse-only results cannot be prepended further. */
    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult =
        error("Cannot prepend to ParseCheckResult! This is a bug.")
}
