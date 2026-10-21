package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

/**
 * Parameter that accepts any argument parsed by a converter function.
 *
 * @param T the parsed argument type.
 * @param command the owning command.
 * @param argumentType display name used in usage and error messages.
 * @param fromString parser that converts raw argument strings.
 */
class AnyParam<T>(
    command: Command<*>?,
    /** Argument type name used in errors and usage. */
    private val argumentType: String,
    /** Parser converting raw strings into typed values. */
    private val fromString: Function1<String?, out T?>,
) : BaseParam(command) {

    /** Parses this argument and returns either a parse result or an error. */
    override fun checkParse(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        val parsed =
            fromString.apply(args[offset])
                ?: return ErrorCheckResult(
                    offset,
                    "§6invalid §3$argumentType§6: §b${args[offset]}§r",
                )
        return ParseCheckResult(offset, argumentType, parsed, true)
    }

    /** Returns completions for this parameter. */
    override fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?> = mutableListOf()
}
