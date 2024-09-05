package org.oddlama.vane.regions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.literal;

import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;

@Name("region")
@Aliases({ "regions", "rg" })
public class Region extends Command<Regions> {

	public Region(Context<Regions> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
		.then(help())
		.requires(ctx -> ctx.getSender() instanceof Player)
		.executes(ctx -> {open_menu((Player) ctx.getSource().getSender()); return SINGLE_SUCCESS;});
	}

	private void open_menu(Player player) {
		get_module().menus.main_menu.create(player).open(player);
	}
}
