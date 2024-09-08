package org.oddlama.vane.admin.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Name("gamemode")
@Aliases({ "gm" })
public class Gamemode extends Command<Admin> {

	@LangMessage
	private TranslatedMessage lang_set;

	public Gamemode(Context<Admin> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
			.then(help())

			.executes(ctx -> {toggle_gamemode_self((Player) ctx.getSource().getSender()); return SINGLE_SUCCESS;})
			
			.then(argument("game_mode", ArgumentTypes.gameMode())
				.executes(ctx -> { set_gamemode_self((Player) ctx.getSource().getSender(), ctx.getArgument("game_mode", GameMode.class)); return SINGLE_SUCCESS;})
				.then(argument("player", ArgumentTypes.player())
					.executes(ctx -> {
						set_gamemode(ctx.getSource().getSender(), ctx.getArgument("game_mode", GameMode.class), player(ctx));
						return SINGLE_SUCCESS;
					})
				)
			)
			.then(argument("player", ArgumentTypes.player())
				.executes(ctx -> {
					toggle_gamemode_player(ctx.getSource().getSender(), player(ctx));
					
					return SINGLE_SUCCESS;
				})
			);
	}

	private Player player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException{
		return ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).get(0);
	}

	private void toggle_gamemode_self(Player player) {
		toggle_gamemode_player(player, player);
	}

	private void toggle_gamemode_player(CommandSender sender, Player player) {
		set_gamemode(sender, player.getGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE, player);
	}

	private void set_gamemode_self(Player player, GameMode mode) {
		set_gamemode(player, mode, player);
	}

	private void set_gamemode(CommandSender sender, GameMode mode, Player player) {
		player.setGameMode(mode);
		lang_set.send(sender, player.displayName().color(NamedTextColor.AQUA), Component.text(mode.name(), NamedTextColor.GREEN));
	}
}
