package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.World
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.command.Command as VaneCommand
import org.oddlama.vane.core.command.argumentType.TimeValueArgumentType
import org.oddlama.vane.core.command.enums.TimeValue
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.WorldUtil

/**
 * Command for changing the world time smoothly.
 *
 * Usage supports changing time in the sender's world or in an explicitly selected world.
 */
@Name("time")
class Time(context: Context<Admin?>) : VaneCommand<Admin?>(context) {
    private val admin: Admin
        get() = requireNotNull(module)

    /** Builds the command tree for smooth time changes. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .then(help())
            .then(
                Commands.argument<TimeValue>("time", TimeValueArgumentType.timeValue())
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val time = timeValue(ctx)
                        if (sender is Player) {
                            setTimeCurrentWorld(sender, time)
                            Command.SINGLE_SUCCESS
                        } else {
                            // Console or non-player senders must specify a world explicitly.
                            sender.sendMessage(
                                "This command must be run by a player or you must specify a world."
                            )
                            0
                        }
                    }
                    .then(
                        Commands.argument("world", ArgumentTypes.world()).executes { ctx ->
                            setTime(timeValue(ctx), ctx.getArgument("world", World::class.java))
                            Command.SINGLE_SUCCESS
                        }
                    )
            )

    /** Returns the parsed time argument from the command context. */
    private fun timeValue(ctx: CommandContext<CommandSourceStack>): TimeValue =
        ctx.getArgument("time", TimeValue::class.java)

    /** Applies the selected time in the player's current world. */
    private fun setTimeCurrentWorld(player: Player, time: TimeValue) =
        WorldUtil.changeTimeSmoothly(player.world, admin, time.ticks.toLong(), 100)

    /** Applies the selected time in a specific world. */
    private fun setTime(time: TimeValue, world: World) =
        WorldUtil.changeTimeSmoothly(world, admin, time.ticks.toLong(), 100)
}
