package org.oddlama.imex.core;

import java.lang.StringBuilder;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
	public long expected_version = -1;
	public void expected_version(long expected_version) {
		this.expected_version = expected_version;
	}

	public void generate_yaml(StringBuilder builder) {
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
