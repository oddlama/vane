package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.WeatherArgumentType;
import org.oddlama.vane.core.command.enums.WeatherValue;
import org.oddlama.vane.core.module.Context;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

@Name("weather")
public class Weather extends Command<Admin> {

	public Weather(Context<Admin> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
			.then(help())
			.then(argument("weather", WeatherArgumentType.weather())
				.executes(ctx -> { set_weather_current_world((Player) ctx.getSource().getSender(), weather(ctx)); return SINGLE_SUCCESS;})
				.then(argument("world", ArgumentTypes.world())
					.executes(ctx -> { set_weather(ctx.getSource().getSender(), weather(ctx), ctx.getArgument("world", World.class)); return SINGLE_SUCCESS; })
				)
			);
	}

	private WeatherValue weather(CommandContext<CommandSourceStack> ctx){
		return ctx.getArgument("weather", WeatherValue.class);
	}

	private void set_weather_current_world(Player player, WeatherValue w) {
		set_weather(player, w, player.getWorld());
	}

	private void set_weather(CommandSender sender, WeatherValue w, World world) {
		world.setStorm(w.storm());
		world.setThundering(w.thunder());
	}
}
