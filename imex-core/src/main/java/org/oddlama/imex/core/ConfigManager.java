package org.oddlama.imex.core;

import java.lang.StringBuilder;
import java.util.logging.Logger;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
	public long expected_version() {
		return 1;
	}

	public void compile(Module module) {
		getAllFields(module.getClass()).stream()
			.forEach(field -> { System.out.println(field.getName()); });
	}

	public void generate_yaml(StringBuilder builder) {
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
