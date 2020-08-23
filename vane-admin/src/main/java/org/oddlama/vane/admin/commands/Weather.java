package org.oddlama.vane.admin.commands;

import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;

@Name("weather")
public class Weather extends Command<Admin> {
	public enum WeatherValue {
		clear(false, false),
		sun(false, false),
		rain(true, false),
		storm(true, false),
		thunder(true, true);

		private boolean is_storm;
		private boolean is_thunder;
		private WeatherValue(boolean is_storm, boolean is_thunder) {
			this.is_storm = is_storm;
			this.is_thunder = is_thunder;
		}

		public boolean storm() { return is_storm; }
		public boolean thunder() { return is_thunder; }
	}

	public Weather(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		var time = params().choice("weather", List.of(WeatherValue.values()), t -> t.name()).ignore_case();
		time.exec_player(this::set_weather_current_world);
		time.choose_world().exec(this::set_weather);
	}

	private void set_weather_current_world(Player player, WeatherValue w) {
		set_weather(player, w, player.getWorld());
	}

	private void set_weather(CommandSender sender, WeatherValue w, World world) {
		world.setStorm(w.storm());
		world.setThundering(w.thunder());
	}
}
