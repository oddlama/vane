package org.oddlama.imex.core;

import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

public class LangManager {
	public long expected_version = -1;

	public void compile(Module module) {
		expected_version = 0;
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
