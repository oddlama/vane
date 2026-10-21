package org.oddlama.vane.core.command.params

import java.lang.reflect.Method
import java.util.logging.LogRecord
import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Executor
import org.oddlama.vane.core.command.Param
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ExecutorCheckResult
import org.oddlama.vane.core.functional.ErasedFunctor
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.GenericsFinder

/**
 * Sentinel parameter that executes a bound functor when argument parsing is complete.
 *
 * @param T executor functor type.
 * @param command the owning command.
 * @param function bound execution functor.
 * @param checkRequirements predicate evaluated before invocation.
 * @param skipArgumentCheck predicate to skip runtime argument-type checks by index.
 */
class SentinelExecutorParam<T>
@JvmOverloads
constructor(
    command: Command<*>?,
    /** Bound executor functor instance. */
    private val function: T?,
    /** Predicate checked before invocation. */
    private val checkRequirements: Function1<CommandSender?, Boolean?> = Function1 { true },
    /** Predicate deciding which argument indices skip strict type checks. */
    private val skipArgumentCheck: Function1<Int?, Boolean?> = Function1 { false },
) : BaseParam(command), Executor {

    /** Validates that parsed arguments match the functor signature. */
    private fun checkSignature(method: Method, args: List<Any?>) {
        if (args.size != method.parameters.size) {
            throw RuntimeException(
                "Invalid command functor ${method.declaringClass.name}::${method.name}!" +
                    "\nFunctor takes ${method.parameters.size} parameters, but ${args.size} were given." +
                    "\nRequired: ${method.parameters.map { it.type.name }}" +
                    "\nGiven: ${args.map { it!!.javaClass.name }}"
            )
        }
        for (i in args.indices) {
            if (skipArgumentCheck.apply(i) == true) continue
            val needs = method.parameters[i].type
            val got = args[i]!!.javaClass
            if (!needs.isAssignableFrom(got)) {
                throw RuntimeException(
                    "Invalid command functor ${method.declaringClass.name}::${method.name}!" +
                        "\nArgument ${i + 1} (${needs.name}) is not assignable from ${got.name}"
                )
            }
        }
    }

    /** Sentinel parameters are executor nodes. */
    override val isExecutor: Boolean
        get() = true

    /** Invokes the bound functor with parsed command arguments. */
    override fun execute(
        command: Command<*>?,
        sender: CommandSender?,
        parsedArgs: MutableList<Any?>,
    ): Boolean {
        val cmd = requireNotNull(command) { "command must not be null" }
        parsedArgs[0] = sender

        val module = requireNotNull(cmd.module) { "command.module must not be null" }
        val log = requireNotNull(module.core) { "module.core must not be null" }.log
        val savedFilter = log.filter
        log.setFilter { _: LogRecord? -> false }
        val method = (function as GenericsFinder).method()
        log.setFilter(savedFilter)

        checkSignature(method, parsedArgs)

        return checkRequirements.apply(sender) == true &&
            try {
                val result = (function as ErasedFunctor).invoke(parsedArgs)
                result == null || result as? Boolean == true
            } catch (e: Exception) {
                throw RuntimeException(
                    "Error while invoking functor ${method.declaringClass.name}::${method.name}!",
                    e,
                )
            }
    }

    /** Sentinel executors do not accept child parameters. */
    override fun addParam(param: Param) {
        throw RuntimeException("Cannot add element to sentinel executor! This is a bug.")
    }

    /** Sentinel executors are never parsed directly as regular parameters. */
    override fun checkParse(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult? = null

    /** Sentinel executors provide no completions. */
    override fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?> = mutableListOf()

    /** Accepts only when argument count exactly matches expected parse depth. */
    override fun checkAccept(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult {
        val safeArgs = requireNotNull(args) { "args must not be null" }
        return when {
            safeArgs.size > offset ->
                ErrorCheckResult(
                    offset - 1,
                    "§6excess arguments: {${
                    safeArgs.slice(offset until safeArgs.size).joinToString(", ") { "§4$it§6" }
                }}§r",
                )

            safeArgs.size < offset ->
                throw RuntimeException(
                    "Sentinel executor received missing arguments! This is a bug."
                )
            else -> ExecutorCheckResult(offset, this)
        }
    }
}
