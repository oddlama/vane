package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function2

/**
 * Parameter that resolves available choices dynamically from the command sender.
 *
 * @param T choice value type.
 * @param command the owning command.
 * @param argumentType display name used in usage and error messages.
 * @param choices provider of available choices for a sender.
 * @param toString formatter used for completions.
 * @param fromString parser used to resolve raw input.
 */
class DynamicChoiceParam<T>(
    command: Command<*>?,
    /** Argument type name used in errors and usage. */
    private val argumentType: String,
    /** Provides available choices for a sender context. */
    private val choices: Function1<CommandSender?, Collection<T?>?>,
    /** Converts choices to display strings. */
    private val toString: Function2<CommandSender?, T?, String?>,
    /** Parses raw strings to choices in sender context. */
    private val fromString: Function2<CommandSender?, String?, out T?>,
) : BaseParam(command) {

    /** Parses this argument using the dynamic parser function. */
    override fun checkParse(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        return fromString.apply(sender, args[offset])?.let {
            ParseCheckResult(offset, argumentType, it, true)
        } ?: ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §b${args[offset]}§r")
    }

    /** Returns sender-specific completions filtered by the current query. */
    override fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?> {
        val current = args?.getOrNull(offset)?.lowercase() ?: return mutableListOf()
        return choices
            .apply(sender)
            .orEmpty()
            .asSequence()
            .mapNotNull { toString.apply(sender, it) }
            .filter { it.lowercase().contains(current) }
            .toMutableList()
    }
}
