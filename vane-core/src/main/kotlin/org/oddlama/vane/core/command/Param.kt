package org.oddlama.vane.core.command

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.CombinedErrorCheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.command.params.*
import org.oddlama.vane.core.functional.*
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.StorageUtil.namespacedKey

/** Command parameter node interface used for parsing, execution dispatch, and completion. */
interface Param {
    /** Child parameters for this node. */
    val params: MutableList<Param?>

    /** Verifies that the sender is a player and emits a localized error otherwise. */
    fun requirePlayer(sender: CommandSender?): Boolean {
        if (sender !is Player) {
            this.command!!.module!!.core?.langCommandNotAPlayer?.send(sender)
            return false
        }

        return true
    }

    /** Returns whether this node is an executor sentinel. */
    val isExecutor: Boolean
        get() = false

    /** Adds a child parameter node. */
    fun addParam(param: Param) {
        if (param.isExecutor && params.any { it?.isExecutor == true }) {
            throw RuntimeException(
                "Cannot define multiple executors for the same parameter! This is a bug."
            )
        }
        params.add(param)
    }

    /** Adds a player-only executor with one argument. */
    fun <T1> execPlayer(f: Consumer1<T1?>?) {
        addParam(
            SentinelExecutorParam<Consumer1<T1?>?>(command, f, { requirePlayer(it) }, { it == 0 })
        )
    }

    /** Adds a player-only executor with two arguments. */
    fun <T1, T2> execPlayer(f: Consumer2<T1?, T2?>?) {
        addParam(
            SentinelExecutorParam<Consumer2<T1?, T2?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor with three arguments. */
    fun <T1, T2, T3> execPlayer(f: Consumer3<T1?, T2?, T3?>?) {
        addParam(
            SentinelExecutorParam<Consumer3<T1?, T2?, T3?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor with four arguments. */
    fun <T1, T2, T3, T4> execPlayer(f: Consumer4<T1?, T2?, T3?, T4?>?) {
        addParam(
            SentinelExecutorParam<Consumer4<T1?, T2?, T3?, T4?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor with five arguments. */
    fun <T1, T2, T3, T4, T5> execPlayer(f: Consumer5<T1?, T2?, T3?, T4?, T5?>?) {
        addParam(
            SentinelExecutorParam<Consumer5<T1?, T2?, T3?, T4?, T5?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor with six arguments. */
    fun <T1, T2, T3, T4, T5, T6> execPlayer(f: Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?) {
        addParam(
            SentinelExecutorParam<Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with one argument. */
    fun <T1> execPlayer(f: Function1<T1?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function1<T1?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with two arguments. */
    fun <T1, T2> execPlayer(f: Function2<T1?, T2?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function2<T1?, T2?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with three arguments. */
    fun <T1, T2, T3> execPlayer(f: Function3<T1?, T2?, T3?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function3<T1?, T2?, T3?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with four arguments. */
    fun <T1, T2, T3, T4> execPlayer(f: Function4<T1?, T2?, T3?, T4?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function4<T1?, T2?, T3?, T4?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with five arguments. */
    fun <T1, T2, T3, T4, T5> execPlayer(f: Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds a player-only executor function with six arguments. */
    fun <T1, T2, T3, T4, T5, T6> execPlayer(f: Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?>(
                command,
                f,
                { requirePlayer(it) },
                { it == 0 },
            )
        )
    }

    /** Adds an executor with one argument. */
    fun <T1> exec(f: Consumer1<T1?>?) {
        addParam(SentinelExecutorParam<Consumer1<T1?>?>(command, f))
    }

    /** Adds an executor with two arguments. */
    fun <T1, T2> exec(f: Consumer2<T1?, T2?>?) {
        addParam(SentinelExecutorParam<Consumer2<T1?, T2?>?>(command, f))
    }

    /** Adds an executor with three arguments. */
    fun <T1, T2, T3> exec(f: Consumer3<T1?, T2?, T3?>?) {
        addParam(SentinelExecutorParam<Consumer3<T1?, T2?, T3?>?>(command, f))
    }

    /** Adds an executor with four arguments. */
    fun <T1, T2, T3, T4> exec(f: Consumer4<T1?, T2?, T3?, T4?>?) {
        addParam(SentinelExecutorParam<Consumer4<T1?, T2?, T3?, T4?>?>(command, f))
    }

    /** Adds an executor with five arguments. */
    fun <T1, T2, T3, T4, T5> exec(f: Consumer5<T1?, T2?, T3?, T4?, T5?>?) {
        addParam(SentinelExecutorParam<Consumer5<T1?, T2?, T3?, T4?, T5?>?>(command, f))
    }

    /** Adds an executor with six arguments. */
    fun <T1, T2, T3, T4, T5, T6> exec(f: Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?) {
        addParam(SentinelExecutorParam<Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?>(command, f))
    }

    /** Adds an executor function with one argument. */
    fun <T1> exec(f: Function1<T1?, Boolean?>?) {
        addParam(SentinelExecutorParam<Function1<T1?, Boolean?>?>(command, f))
    }

    /** Adds an executor function with two arguments. */
    fun <T1, T2> exec(f: Function2<T1?, T2?, Boolean?>?) {
        addParam(SentinelExecutorParam<Function2<T1?, T2?, Boolean?>?>(command, f))
    }

    /** Adds an executor function with three arguments. */
    fun <T1, T2, T3> exec(f: Function3<T1?, T2?, T3?, Boolean?>?) {
        addParam(SentinelExecutorParam<Function3<T1?, T2?, T3?, Boolean?>?>(command, f))
    }

    /** Adds an executor function with four arguments. */
    fun <T1, T2, T3, T4> exec(f: Function4<T1?, T2?, T3?, T4?, Boolean?>?) {
        addParam(SentinelExecutorParam<Function4<T1?, T2?, T3?, T4?, Boolean?>?>(command, f))
    }

    /** Adds an executor function with five arguments. */
    fun <T1, T2, T3, T4, T5> exec(f: Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?) {
        addParam(SentinelExecutorParam<Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?>(command, f))
    }

    /** Adds an executor function with six arguments. */
    fun <T1, T2, T3, T4, T5, T6> exec(f: Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?>(command, f)
        )
    }

    /** Adds an unconstrained string argument parameter. */
    fun anyString(): Param = any<String?>("string") { str -> str }

    /** Adds an unconstrained argument parameter converted via [fromString]. */
    fun <T> any(argumentType: String, fromString: Function1<String?, out T?>): AnyParam<out T?> {
        val p = AnyParam(this.command, argumentType, fromString)
        addParam(p)
        return p
    }

    /** Adds a fixed string literal parameter. */
    fun fixed(fixed: String?): FixedParam<String?> = fixed(fixed) { str -> str }

    /** Adds a fixed literal parameter. */
    fun <T> fixed(fixed: T?, toString: Function1<T?, String?>): FixedParam<T> {
        val p = FixedParam(this.command, fixed, toString)
        addParam(p)
        return p
    }

    /** Adds a static choice parameter of strings. */
    fun choice(choices: Collection<String?>): Param =
        choice<String?>("choice", choices) { str -> str }

    /** Adds a static choice parameter. */
    fun <T> choice(
        argumentType: String,
        choices: Collection<T?>,
        toString: Function1<T?, String?>,
    ): ChoiceParam<T> {
        val p = ChoiceParam(this.command, argumentType, choices, toString)
        addParam(p)
        return p
    }

    /** Adds a dynamic choice parameter. */
    fun <T> choice(
        argumentType: String,
        choices: Function1<CommandSender?, Collection<T?>?>,
        toString: Function2<CommandSender?, T?, String?>,
        fromString: Function2<CommandSender?, String?, out T?>,
    ): DynamicChoiceParam<T?> {
        val p = DynamicChoiceParam(this.command, argumentType, choices, toString, fromString)
        addParam(p)
        return p
    }

    /** Adds a dynamic choice parameter for loaded modules. */
    fun chooseModule(): DynamicChoiceParam<Module<*>?> {
        return choice<Module<*>?>(
            "module",
            { _ -> this.command!!.module!!.core?.modules ?: mutableSetOf<Module<*>>() },
            { _, m -> m!!.annotationName },
            Function2 { _, str ->
                val core = this.command!!.module!!.core ?: return@Function2 null
                core.modules.firstOrNull { it?.annotationName.equals(str, ignoreCase = true) }
            },
        )
    }

    /** Adds a dynamic choice parameter for worlds. */
    fun chooseWorld(): DynamicChoiceParam<World?> {
        return choice<World?>(
            "world",
            { _ -> this.command!!.module!!.server.worlds },
            { _, w -> w!!.name.lowercase() },
            Function2 { _, str ->
                this.command!!.module!!.server.worlds.firstOrNull {
                    it.name.equals(str, ignoreCase = true)
                }
            },
        )
    }

    /** Adds a dynamic choice parameter for known offline players. */
    fun chooseAnyPlayer(): DynamicChoiceParam<OfflinePlayer?> {
        return choice<OfflinePlayer?>(
            "any_player",
            { _ -> this.command!!.module!!.offlinePlayersWithValidName.toMutableList() },
            { _, p -> p!!.name },
            Function2 { _, str ->
                this.command!!.module!!.offlinePlayersWithValidName.firstOrNull {
                    it.name.equals(str, ignoreCase = true)
                }
            },
        )
    }

    /** Adds a dynamic choice parameter for currently online players. */
    fun chooseOnlinePlayer(): DynamicChoiceParam<Player?> {
        return choice<Player?>(
            "online_player",
            { _ -> this.command!!.module!!.server.onlinePlayers.toMutableList() },
            { _, p -> p!!.name },
            Function2 { _, str ->
                this.command!!.module!!.server.onlinePlayers.firstOrNull {
                    it.name.equals(str, ignoreCase = true)
                }
            },
        )
    }

    /**
     * Adds a dynamic choice parameter for registered permissions.
     *
     * TODO: filter results based on a previously specified player.
     */
    fun choosePermission(): DynamicChoiceParam<Permission?> {
        return choice<Permission?>(
            "permission",
            { _ -> this.command!!.module!!.server.pluginManager.permissions },
            { _, p -> p!!.name },
            Function2 { _, str ->
                this.command!!.module!!.server.pluginManager.getPermission(str!!)
            },
        )
    }

    /** Adds a static choice parameter for game modes. */
    fun chooseGamemode(): ChoiceParam<GameMode?> =
        choice<GameMode?>("gamemode", GameMode.entries) { m -> m!!.name.lowercase() }.ignoreCase()

    /** Adds a dynamic choice parameter for enchantments. */
    fun chooseEnchantment(
        filter: Function2<CommandSender?, Enchantment?, Boolean?> = Function2 { _, _ -> true }
    ): DynamicChoiceParam<Enchantment?> {
        return choice<Enchantment?>(
            "enchantment",
            { _ ->
                RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .filter { filter.apply(null, it) == true }
                    .toMutableList()
            },
            { _, e -> e!!.key.toString() },
            Function2 { _, str ->
                val parts = str?.split(":") ?: return@Function2 null
                if (parts.size != 2) return@Function2 null
                val e =
                    RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT)
                        .get(namespacedKey(parts[0], parts[1]))
                e.takeIf { filter.apply(null, it) == true }
            },
        )
    }

    /** Delegates acceptance checking to child parameters. */
    fun checkAcceptDelegate(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): CheckResult? {
        require(params.isNotEmpty()) { "Encountered parameter without sentinel! This is a bug." }

        val results = params.mapNotNull { it!!.checkAccept(sender, args, offset + 1) }

        results
            .firstOrNull { it.good() }
            ?.let {
                return it
            }

        val maxDepth = results.maxOf { it.depth() }
        val errors = results.filter { it.depth() == maxDepth }.filterIsInstance<ErrorCheckResult>()

        return if (errors.size == 1) errors[0] else CombinedErrorCheckResult(errors)
    }

    /** Parses this parameter and then delegates acceptance checking to children. */
    fun checkAccept(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult? {
        val result = checkParse(sender, args, offset)
        if (result !is ParseCheckResult) {
            return result
        }

        return checkAcceptDelegate(sender, args, offset)!!.prepend(
            result.argumentType,
            result.parsed,
            result.includeParam,
        )
    }

    /** Delegates completions to child parameters. */
    fun buildCompletionsDelegate(
        sender: CommandSender?,
        args: Array<String?>,
        offset: Int,
    ): MutableList<String?> {
        require(params.isNotEmpty()) { "Encountered parameter without sentinel! This is a bug." }

        return params
            .flatMap { it!!.buildCompletions(sender, args, offset + 1) ?: emptyList() }
            .toMutableList()
    }

    /** Builds completions for this parameter node and its children. */
    fun buildCompletions(
        sender: CommandSender?,
        args: Array<String?>,
        offset: Int,
    ): MutableList<String?>? {
        return if (offset < args.size - 1) {
            if (checkParse(sender, args, offset) is ParseCheckResult) {
                buildCompletionsDelegate(sender, args, offset)
            } else {
                mutableListOf()
            }
        } else {
            completionsFor(sender, args, offset)
        }
    }

    /** Returns completions for this specific parameter. */
    fun completionsFor(
        sender: CommandSender?,
        args: Array<String?>?,
        offset: Int,
    ): MutableList<String?>?

    /** Parses this parameter at the given argument offset. */
    fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult?

    /** Returns the owning command. */
    val command: Command<*>?
}
