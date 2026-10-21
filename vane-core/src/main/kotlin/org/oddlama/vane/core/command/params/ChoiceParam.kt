package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

/**
 * Parameter that matches one value from a static choice collection.
 *
 * @param T choice value type.
 * @param command the owning command.
 * @param argumentType display name used in usage and error messages.
 * @param choices static choice set.
 * @param toString formatter used for parsing and completions.
 */
class ChoiceParam<T>(
    command: Command<*>?,
    /** Argument type name used in errors and usage. */
    private val argumentType: String,
    /** Static list of available choices. */
    private val choices: Collection<T?>,
    /** Converts choices to display strings. */
    private val toString: Function1<T?, String?>,
) : BaseParam(command) {
    /** Whether parsing and completion matching should ignore case. */
    private var ignoreCase = false

    /** Lookup table from display string to choice value. */
    private val fromString: MutableMap<String, T?> =
        choices.associateByTo(mutableMapOf()) { (toString.apply(it) ?: "") }

    /** Enables case-insensitive matching. */
    fun ignoreCase(): ChoiceParam<T> {
        ignoreCase = true
        fromString.clear()
        choices.forEach { fromString[toString.apply(it)?.lowercase() ?: ""] = it }
        return this
    }

    /** Parses this argument against the static choice set. */
    override fun checkParse(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        val argVal =
            args[offset] ?: return ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §bnull§r")
        return parse(argVal)?.let { ParseCheckResult(offset, argumentType, it, true) }
            ?: ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §b$argVal§r")
    }

    /** Returns completions filtered by the current argument prefix. */
    override fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?> {
        val query = args?.getOrNull(offset) ?: return mutableListOf()
        val search = if (ignoreCase) query.lowercase() else query
        return choices
            .asSequence()
            .mapNotNull { toString.apply(it) }
            .filter { str ->
                if (ignoreCase) str.lowercase().contains(search) else str.contains(search)
            }
            .toMutableList()
    }

    /** Parses a raw argument string into a choice value. */
    private fun parse(arg: String): T? = fromString[if (ignoreCase) arg.lowercase() else arg]
}
