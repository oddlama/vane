package org.oddlama.vane.admin.commands;

import static org.oddlama.vane.util.WorldUtil.change_time_smoothly;

import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;

@Name("time")
public class Time extends Command<Admin> {
	public enum TimeValue {
		dawn(23000),
		day(1000),
		noon(6000),
		afternoon(9000),
		dusk(13000),
		night(14000),
		midnight(18000);

		private int ticks;
		private TimeValue(int ticks) {
			this.ticks = ticks;
		}

		public int ticks() { return ticks; }
	}

	public Time(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		var time = params().choice("time", List.of(TimeValue.values()), t -> t.name()).ignore_case();
		time.exec_player(this::set_time_current_world);
		time.choose_world().exec(this::set_time);
	}

	private void set_time_current_world(Player player, TimeValue t) {
		change_time_smoothly(player.getWorld(), get_module(), t.ticks(), 100);
	}

	private void set_time(CommandSender sender, TimeValue t, World world) {
		change_time_smoothly(world, get_module(), t.ticks(), 100);
	}
}
