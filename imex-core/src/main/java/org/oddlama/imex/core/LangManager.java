package org.oddlama.imex.core;

import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

public class LangManager {
	Module module;

	public LangManager(Module module) {
		this.module = module;
	}

	public long expected_version() {
		return 1;
	}

	public void compile(Module module) {
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
