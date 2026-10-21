package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.command.CommandSender
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.admin.AutostopGroup
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.util.ticksToMs

/** Command interface for autostop status, scheduling, and abortion. */
@Name("autostop")
class Autostop(private val autostop: AutostopGroup) :
    org.oddlama.vane.core.command.Command<Admin?>(autostop) {
    /** Builds the command tree for autostop control operations. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .executes { ctx ->
                status(ctx.source.sender)
                Command.SINGLE_SUCCESS
            }
            .then(help())
            .then(
                Commands.literal("status").executes { ctx ->
                    status(ctx.source.sender)
                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("abort").executes { ctx ->
                    abort(ctx.source.sender)
                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("schedule")
                    .executes { ctx ->
                        schedule(ctx.source.sender)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("time", ArgumentTypes.time()).executes { ctx ->
                            scheduleDelay(
                                ctx.source.sender,
                                ctx.getArgument("time", Int::class.java),
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
            )
    }

    /** Sends the current autostop status to the sender. */
    private fun status(sender: CommandSender?) {
        autostop.status(sender)
    }

    /** Cancels an active autostop schedule. */
    private fun abort(sender: CommandSender?) {
        autostop.abort(sender)
    }

    /** Starts autostop with configured default delay. */
    private fun schedule(sender: CommandSender?) {
        autostop.schedule(sender)
    }

    /** Starts autostop using a delay parsed from the command input. */
    private fun scheduleDelay(sender: CommandSender?, delay: Int) {
        autostop.schedule(sender, ticksToMs(delay.toLong()))
    }
}
