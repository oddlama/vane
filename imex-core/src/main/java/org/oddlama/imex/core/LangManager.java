package org.oddlama.imex.core;

import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

public class LangManager {
	public long expected_version = -1;
	public void expected_version(long expected_version) {
		this.expected_version = expected_version;
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
