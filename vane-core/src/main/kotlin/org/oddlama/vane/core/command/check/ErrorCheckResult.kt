package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

/**
 * Failed parse/match result carrying an error message.
 *
 * @param depth parse depth where the failure occurred.
 * @param message localized or formatted error message.
 */
open class ErrorCheckResult(private val depth: Int, private val message: String) : CheckResult {
    /** Rendered chain of argument types matched before this error. */
    private var argChain = ""

    /** Returns the parse depth for this result. */
    override fun depth(): Int = depth

    /** Error results are always unsuccessful. */
    override fun good(): Boolean = false

    /** Applies this error using root indentation. */
    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        apply(command, sender, "")

    /** Sends this error message to the sender. */
    open fun apply(command: Command<*>?, sender: CommandSender?, indent: String): Boolean {
        val prefix = indent.ifEmpty { "${indent}§cerror: " }
        sender?.sendMessage("${prefix}§6${argChain}${message}")
        return false
    }

    /** Prepends argument context to the displayed error chain. */
    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult {
        argChain = "§3${argumentType}§6 → $argChain"
        return this
    }
}
