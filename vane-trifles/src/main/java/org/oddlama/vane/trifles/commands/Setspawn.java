package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.StorageUtil;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;

@Name("setspawn")
public class Setspawn extends Command<Trifles> {
	public static final NamespacedKey IS_SPAWN_WORLD = StorageUtil.namespaced_key("vane", "is_spawn_world");

	public Setspawn(Context<Trifles> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return super.get_command_base()
			.requires(ctx -> ctx.getSender() instanceof Player)
			.then(help())
			.executes(ctx -> {set_spawn((Player) ctx.getSource().getSender()); return SINGLE_SUCCESS;})
		;
	}

	private void set_spawn(Player player) {
		final var loc = player.getLocation();

		// Unset spawn tag in all worlds
		for (final var world : get_module().getServer().getWorlds()) {
			world.getPersistentDataContainer().remove(IS_SPAWN_WORLD);
		}

		// Set spawn and mark as the default world
		final var world = player.getWorld();
		world.setSpawnLocation(loc);
		world.getPersistentDataContainer().set(IS_SPAWN_WORLD, PersistentDataType.INTEGER, 1);
		player.sendMessage("§aSpawn §7set!");
	}
}
