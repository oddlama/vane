package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

/**
 * Parameter that matches a single fixed literal value.
 *
 * @param T fixed value type.
 * @param command the owning command.
 * @param fixedArg fixed value matched by this parameter.
 * @param toString formatter for the fixed value.
 */
class FixedParam<T>(
    command: Command<*>?,
    private val fixedArg: T?,
    toString: Function1<T?, String?>,
) : BaseParam(command) {
    /** String representation of [fixedArg] used for parsing and completion. */
    private val fixedArgStr: String? = toString.apply(fixedArg)

    /** Whether the fixed parameter should be forwarded to executors. */
    private var includeParam = false

    /** Whether matching is case-insensitive. */
    private var ignoreCase = false

    /** Enables case-insensitive matching. */
    fun ignoreCase(): FixedParam<T> = apply { ignoreCase = true }

    /** Includes this matched fixed value in executor arguments. */
    fun includeParam(): FixedParam<T> = apply { includeParam = true }

    /** Parses the fixed literal argument. */
    override fun checkParse(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$fixedArgStr§r")
        val argVal =
            args[offset]
                ?: return ErrorCheckResult(
                    offset,
                    "§6invalid argument: expected §3$fixedArgStr§6 got §bnull§r",
                )
        return parse(argVal)?.let { ParseCheckResult(offset, fixedArgStr ?: "", it, includeParam) }
            ?: ErrorCheckResult(
                offset,
                "§6invalid argument: expected §3$fixedArgStr§6 got §b$argVal§r",
            )
    }

    /** Returns the fixed literal as the only completion candidate. */
    override fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?> = mutableListOf(fixedArgStr)

    /** Matches a raw argument against [fixedArgStr]. */
    private fun parse(arg: String): T? = fixedArg.takeIf {
        arg.equals(fixedArgStr, ignoreCase = ignoreCase)
    }
}
