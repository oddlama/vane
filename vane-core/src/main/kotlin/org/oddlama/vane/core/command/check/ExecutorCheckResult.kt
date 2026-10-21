package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Executor

/**
 * Successful match result that holds an executable callback and parsed arguments.
 *
 * @param depth parse depth where this executor matched.
 * @param executor command executor callback.
 */
class ExecutorCheckResult(private val depth: Int, private val executor: Executor) : CheckResult {
    /** Parsed arguments collected while prepending match results. */
    private val parsedArgs = mutableListOf<Any?>()

    /** Returns the parse depth for this result. */
    override fun depth(): Int = depth

    /** Executor results are always successful. */
    override fun good(): Boolean = true

    /** Executes the stored callback with collected arguments. */
    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        executor.execute(command, sender, parsedArgs)

    /** Prepends a parsed argument into the execution argument list when requested. */
    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult {
        if (include) parsedArgs.add(0, parsedArg)
        return this
    }
}
