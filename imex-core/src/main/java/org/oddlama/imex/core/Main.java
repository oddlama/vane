package org.oddlama.imex.core;

import java.util.ArrayList;
import java.util.Set;

import org.reflections.Reflections;

import org.bukkit.plugin.java.JavaPlugin;

import org.oddlama.imex.annotation.ImexModule;

public class Main extends JavaPlugin {
	private ArrayList<Module> modules = new ArrayList<>();

	@Override
	public void onLoad() {
		Reflections reflections = new Reflections("org.oddlama.imex");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ImexModule.class);

		for (Class<?> module : annotated) {
			System.out.println("ja moin " + module.toString());
		}
	}

	@Override
	public void onEnable() {
		// Connect to database

		// Enable dynmap integration

		// Register listeners

		// Register commands

		// Schedule shutdown if there are no players
		if (getServer().getOnlinePlayers().isEmpty()) {
		}
	}

	@Override
	public void onDisable() {
		// Unregister commands

		// Unregister listeners

		// Disable dynmap integration

		// Close database
	}
}
