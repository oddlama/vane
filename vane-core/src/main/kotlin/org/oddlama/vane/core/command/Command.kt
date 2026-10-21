package org.oddlama.vane.core.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.command.VaneCommand
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.command.params.AnyParam
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Base class for vane commands integrating Bukkit command handling and brigadier nodes.
 *
 * @param T the owning module type.
 * @param context the component context.
 * @param permissionDefault the default permission state for the command permission.
 */
@VaneCommand
abstract class Command<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>,
    permissionDefault: PermissionDefault? = PermissionDefault.OP,
) : ModuleComponent<T?>(null) {
    /**
     * Bukkit command wrapper delegating execution and completion to the parameter tree.
     *
     * @param name the command name.
     */
    inner class BukkitCommand(name: String) :
        org.bukkit.command.Command(name), PluginIdentifiableCommand {
        init {
            permission = this@Command.permission.name
        }

        /** Returns the localized usage string. */
        override fun getUsage(): String = this@Command.langUsage.str("§7/§3$name")

        /** Returns the localized command description. */
        override fun getDescription(): String = this@Command.langDescription.str()

        /** Returns the owning plugin. */
        override fun getPlugin(): Plugin = this@Command.module!!

        /** Executes this command for a sender. */
        @Throws(IllegalArgumentException::class)
        override fun execute(
            sender: CommandSender,
            commandLabel: String,
            args: Array<String>,
        ): Boolean {
            if (!sender.hasPermission(this@Command.permission)) {
                module!!.core?.langCommandPermissionDenied?.send(sender)
                return true
            }

            val combined = arrayOfNulls<String>(args.size + 1)
            combined[0] = commandLabel
            args.forEachIndexed { i, arg -> combined[i + 1] = arg }

            return try {
                rootParam.checkAccept(sender, combined, 0)?.apply(this@Command, sender) ?: false
            } catch (e: Exception) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                )
                throw e
            }
        }

        /** Builds tab-completion suggestions for the current command input. */
        @Throws(IllegalArgumentException::class)
        override fun tabComplete(
            sender: CommandSender,
            alias: String,
            args: Array<String>,
        ): MutableList<String> {
            if (!sender.hasPermission(permission!!)) return mutableListOf()

            val combined = arrayOfNulls<String>(args.size + 1)
            combined[0] = alias
            args.forEachIndexed { i, arg -> combined[i + 1] = arg }

            return try {
                rootParam.buildCompletions(sender, combined, 0)?.filterNotNull()?.toMutableList()
                    ?: mutableListOf()
            } catch (e: Exception) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                )
                throw e
            }
        }
    }

    /** Localized usage line. */
    @LangMessage lateinit var langUsage: TranslatedMessage

    /** Localized command description. */
    @LangMessage lateinit var langDescription: TranslatedMessage

    /** Localized help text body. */
    @LangMessage lateinit var langHelp: TranslatedMessage

    /** Command name from the `@Name` annotation. */
    val name: String

    /** Permission required to execute this command. */
    private val permission: Permission

    /** Bukkit command instance registered for this command. */
    private var bukkitCommand: BukkitCommand

    /** Root parameter node for command parsing and execution. */
    private val rootParam: AnyParam<String?>

    /** Backing brigadier literal node used by [getCommandBase]. */
    private var commandBaseField: LiteralArgumentBuilder<CommandSourceStack>? = null

    /** Optional additional aliases from `@Aliases`. */
    private var aliases: Aliases?

    /** Returns the brigadier literal root builder for this command. */
    open fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> = commandBaseField!!

    init {
        var context = context
        name = javaClass.getAnnotation(Name::class.java).value
        val groupName = "Command${name.replaceFirstChar { it.uppercase() }}"
        context = context.group(groupName, "Enable command $name")
        setContext(context)

        permission =
            Permission(
                "vane.${module!!.annotationName}.commands.$name",
                "Allow access to /$name",
                permissionDefault,
            )
        module!!.registerPermission(permission)
        module!!.permissionCommandCatchallModule?.let { permission.addParent(it, true) }
        module!!.core?.let { permission.addParent(it.permissionCommandCatchall, true) }

        module!!.addConsolePermission(permission)

        rootParam = AnyParam(this, "/$name", Function1 { it })

        bukkitCommand =
            BukkitCommand(name).also {
                it.label = name
                it.name = name
            }

        aliases = javaClass.getAnnotation(Aliases::class.java)
        commandBaseField = Commands.literal(name)
        aliases?.let { bukkitCommand.setAliases(it.value.toMutableList()) }
    }

    /** Returns the registered Bukkit command instance. */
    fun getBukkitCommand(): BukkitCommand = bukkitCommand

    /** Returns the command permission node. */
    fun getPermission(): String = permission.name

    /** Returns the brigadier command namespace prefix. */
    val prefix: String
        get() = "vane:${module!!.annotationName}"

    /** Returns the root command parameter. */
    fun params(): Param = rootParam

    /**
     * Returns the brigadier command node with permission requirements applied. Built lazily so the
     * shared builder is only mutated once, preventing cumulative corruption when
     * LifecycleEvents.COMMANDS replays late handlers.
     */
    val command: LiteralCommandNode<CommandSourceStack> by lazy {
        val cmd = getCommandBase()
        val existingRequirement = cmd.requirement
        cmd.requires { stack ->
                stack.sender.hasPermission(permission) && existingRequirement.test(stack)
            }
            .build()
    }

    /** Returns configured aliases. */
    fun getAliases(): MutableList<String> = aliases?.value?.toMutableList() ?: mutableListOf()

    /** Registers this command with the owning module. */
    override fun onEnable() {
        module!!.registerCommand(this)
    }

    /** Unregisters this command from the owning module. */
    override fun onDisable() {
        module!!.unregisterCommand(this)
    }

    /** Sends command usage/help lines to a sender. */
    fun printHelp(sender: CommandSender?) {
        langUsage.send(sender, "§7/§3$name")
        langHelp.send(sender)
    }

    /** Brigadier handler that sends command help. */
    fun printHelp2(ctx: CommandContext<CommandSourceStack>): Int {
        langUsage.send(ctx.source.sender, "§7/§3$name")
        langHelp.send(ctx.source.sender)
        return Command.SINGLE_SUCCESS
    }

    /** Returns a `help` literal that prints command help. */
    fun help(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal("help").executes { ctx ->
            printHelp2(ctx)
            Command.SINGLE_SUCCESS
        }
}
