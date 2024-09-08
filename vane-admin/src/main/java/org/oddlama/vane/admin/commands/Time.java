package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static org.oddlama.vane.util.WorldUtil.change_time_smoothly;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.TimeValueArgumentType;
import org.oddlama.vane.core.command.enums.TimeValue;
import org.oddlama.vane.core.module.Context;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

@Name("time")
public class Time extends Command<Admin> {

	public Time(Context<Admin> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
			.then(help())
			.then(argument("time", TimeValueArgumentType.timeValue())
				.executes(ctx -> { set_time_current_world((Player) ctx.getSource().getSender(), time_value(ctx)); return SINGLE_SUCCESS;})
				.then(argument("world", ArgumentTypes.world())
					.executes(ctx -> { set_time(time_value(ctx), ctx.getArgument("world", World.class)); return SINGLE_SUCCESS;})
				)
			)
		;
	}

	private TimeValue time_value(CommandContext<CommandSourceStack> ctx) {
		return ctx.getArgument("time", TimeValue.class);
	}

	private void set_time_current_world(Player player, TimeValue t) {
		change_time_smoothly(player.getWorld(), get_module(), t.ticks(), 100);
	}

	private void set_time(TimeValue t, World world) {
		change_time_smoothly(world, get_module(), t.ticks(), 100);
	}
}
