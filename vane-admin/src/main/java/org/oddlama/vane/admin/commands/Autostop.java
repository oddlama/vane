package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.admin.AutostopGroup;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.util.Conversions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

@Name("autostop")
public class Autostop extends Command<Admin> {

	AutostopGroup autostop;

	public Autostop(AutostopGroup context) {
		super(context);
		this.autostop = context;
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
			.executes(ctx -> { status(ctx.getSource().getSender()); return SINGLE_SUCCESS; })
			.then(help())
			.then(literal("status").executes(ctx -> { status(ctx.getSource().getSender()); return SINGLE_SUCCESS; }))
			.then(literal("abort").executes(ctx -> { abort(ctx.getSource().getSender()); return SINGLE_SUCCESS;}))
			.then(literal("schedule")
				.executes(ctx -> { schedule(ctx.getSource().getSender()); return SINGLE_SUCCESS; })
				.then(argument("time", ArgumentTypes.time())
					.executes(ctx -> { schedule_delay(ctx.getSource().getSender(), ctx.getArgument("time", Integer.class)); return SINGLE_SUCCESS;}))
			);
	}

	private void status(CommandSender sender) {
		autostop.status(sender);
	}

	private void abort(CommandSender sender) {
		autostop.abort(sender);
	}

	private void schedule(CommandSender sender) {
		autostop.schedule(sender);
	}

	private void schedule_delay(CommandSender sender, int delay){
		autostop.schedule(sender, Conversions.ticks_to_ms(delay));
	}
}
