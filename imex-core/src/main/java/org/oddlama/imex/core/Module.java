package org.oddlama.imex.core;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.oddlama.imex.annotation.ConfigString;

@ConfigString(name = "version", def = "1", desc = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.")
@ConfigString(name = "lang", def = "inherit", desc = "The language for this module. Specifying 'inherit' will use the value set for imex-core.")
public abstract class Module extends ModuleBase {
	@Override
	public String get_config_lang() {
		return "en";
		//return config_lang;
	}

	public void register_listener(Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	public void schedule_task(Runnable task, long delay_ticks) {
		getServer().getScheduler().runTaskLater(this, task, delay_ticks);
	}

	public void schedule_next_tick(Runnable task) {
		getServer().getScheduler().runTask(this, task);
	}
}
