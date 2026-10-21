package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.command.Command as VaneCommand
import org.oddlama.vane.core.command.argumentType.WeatherArgumentType
import org.oddlama.vane.core.command.enums.WeatherValue
import org.oddlama.vane.core.module.Context

/** Command for changing weather in the current or a selected world. */
@Name("weather")
class Weather(context: Context<Admin?>) : VaneCommand<Admin?>(context) {
    /** Builds the command tree for weather changes. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .then(help())
            .then(
                Commands.argument<WeatherValue>("weather", WeatherArgumentType.weather())
                    .executes { ctx ->
                        setWeatherCurrentWorld(ctx.source.sender as Player, weather(ctx))
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("world", ArgumentTypes.world()).executes { ctx ->
                            setWeather(
                                ctx.source.sender,
                                weather(ctx),
                                ctx.getArgument("world", World::class.java),
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
            )

    /** Returns the parsed weather argument from the command context. */
    private fun weather(ctx: CommandContext<CommandSourceStack>): WeatherValue =
        ctx.getArgument("weather", WeatherValue::class.java)

    /** Applies weather in the player's current world. */
    private fun setWeatherCurrentWorld(player: Player, weather: WeatherValue) =
        setWeather(player, weather, player.world)

    /** Applies weather in a specific world. */
    private fun setWeather(
        @Suppress("UNUSED_PARAMETER") sender: CommandSender?,
        weather: WeatherValue,
        world: World,
    ) {
        world.setStorm(weather.storm)
        world.isThundering = weather.thunder
    }
}
