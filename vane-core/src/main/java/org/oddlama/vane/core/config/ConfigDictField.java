package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigDictField extends ConfigField<ConfigDictSerializable> {
	private class EmptyDict implements ConfigDictSerializable {
		@Override
		public Map<String, Object> to_dict() {
			return new HashMap<>();
		}

		@Override
		public void from_dict(final Map<String, Object> dict) {
			// no-op
		}
	}

	public ConfigDict annotation;

	public ConfigDictField(
		final Object owner,
		final Field field,
		final Function<String, String> map_name,
		final ConfigDict annotation
	) {
		super(owner, field, map_name, "dict", annotation.desc());
		this.annotation = annotation;
	}

	@Override
	public ConfigDictSerializable def() {
		final var override = overridden_def();
		if (override != null) {
			return override;
		} else {
			return new EmptyDict();
		}
	}

	@Override
	public boolean metrics() {
		final var override = overridden_metrics();
		if (override != null) {
			return override;
		} else {
			return annotation.metrics();
		}
	}

	@SuppressWarnings("unchecked")
	private void append_list(final StringBuilder builder, final String indent, final String list_key, final List<Object> list) {
		builder.append(indent);
		builder.append(list_key);
		if (list.isEmpty()) {
			builder.append(": []\n");
		} else {
			builder.append(":\n");
			list.forEach(entry -> {
				if (entry instanceof String) {
					builder.append(indent);
					builder.append("  - ");
					builder.append("\"" + escape_yaml(entry.toString()) + "\"");
					builder.append("\n");
				} else if (
					entry instanceof Integer ||
					entry instanceof Long ||
					entry instanceof Float ||
					entry instanceof Double ||
					entry instanceof Boolean
				) {
					builder.append(indent);
					builder.append("  - ");
					builder.append(entry);
					builder.append("\n");
				} else if (entry instanceof Map<?,?>) {
					append_dict(builder, indent + "  ", null, (Map<String, Object>)entry, true);
				} else {
					throw new RuntimeException("Invalid value '" + entry + "' of type " + entry.getClass() + " in mapping of ConfigDictSerializable");
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void append_dict(final StringBuilder builder, final String indent, final String dict_key, final Map<String, Object> dict, final boolean is_list_entry) {
		builder.append(indent);
		if (is_list_entry) {
			builder.append("-");
		} else {
			builder.append(dict_key);
			builder.append(":");
		}
		if (dict.isEmpty()) {
			builder.append(" {}\n");
		} else {
			builder.append("\n");
			dict.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
				if (entry.getValue() instanceof String) {
					builder.append(indent + "  ");
					builder.append(entry.getKey());
					builder.append(": ");
					builder.append("\"" + escape_yaml(entry.getValue().toString()) + "\"");
					builder.append("\n");
				} else if (
					entry.getValue() instanceof Integer ||
					entry.getValue() instanceof Long ||
					entry.getValue() instanceof Float ||
					entry.getValue() instanceof Double ||
					entry.getValue() instanceof Boolean
				) {
					builder.append(indent + "  ");
					builder.append(entry.getKey());
					builder.append(": ");
					builder.append(entry.getValue().toString());
					builder.append("\n");
				} else if (entry.getValue() instanceof Map<?,?>) {
					append_dict(builder, indent + "  ", entry.getKey(), (Map<String, Object>)entry.getValue(), false);
				} else if (entry.getValue() instanceof List<?>) {
					append_list(builder, indent + "  ", entry.getKey(), (List<Object>)entry.getValue());
				} else {
					throw new RuntimeException("Invalid value '" + entry.getValue() + "' of type " + entry.getValue().getClass() + " in mapping of ConfigDictSerializable");
				}
			});
		}
	}

	private void append_dict(final StringBuilder builder, final String indent, final boolean default_definition, final ConfigDictSerializable ser) {
		if (default_definition) {
			append_dict(builder, indent + "# ", "Default", ser.to_dict(), false);
		} else {
			append_dict(builder, indent, basename(), ser.to_dict(), false);
		}
	}

	@Override
	public void generate_yaml(final StringBuilder builder, final String indent, final YamlConfiguration existing_compatible_config) {
		append_description(builder, indent);
		append_dict(builder, indent, true, def());
		final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
			? load_from_yaml(existing_compatible_config)
			: def();
		append_dict(builder, indent, false, def);
	}

	@Override
	public void check_loadable(final YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isConfigurationSection(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected configuration section");
		}
	}

	public ArrayList<Object> load_list_from_yaml(final List<?> raw_list) {
		final var list = new ArrayList<Object>();
		for (var e : raw_list) {
			if (e instanceof ConfigurationSection section) {
				list.add(load_dict_from_yaml(section));
			} else {
				list.add(e);
			}
		}
		return list;
	}

	public HashMap<String, Object> load_dict_from_yaml(final ConfigurationSection section) {
		final var dict = new HashMap<String, Object>();
		for (var subkey : section.getKeys(false)) {
			if (section.isConfigurationSection(subkey)) {
				dict.put(subkey, load_dict_from_yaml(section.getConfigurationSection(subkey)));
			} else if (section.isList(subkey)) {
				dict.put(subkey, load_list_from_yaml(section.getList(subkey)));
			} else if (section.isString(subkey)) {
				dict.put(subkey, section.getString(subkey));
			} else if (section.isInt(subkey)) {
				dict.put(subkey, section.getInt(subkey));
			} else if (section.isDouble(subkey)) {
				dict.put(subkey, section.getDouble(subkey));
			} else if (section.isBoolean(subkey)) {
				dict.put(subkey, section.getBoolean(subkey));
			} else if (section.isLong(subkey)) {
				dict.put(subkey, section.getLong(subkey));
			} else {
				throw new IllegalStateException("Cannot load dict entry '" + yaml_path() + "." + subkey + "': unknown type");
			}
		}
		return dict;
	}

	public ConfigDictSerializable load_from_yaml(final YamlConfiguration yaml) {
		try {
			final var dict = ((ConfigDictSerializable)annotation.cls().getDeclaredConstructor().newInstance());
			dict.from_dict(load_dict_from_yaml(yaml.getConfigurationSection(yaml_path())));
			return dict;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Could not instanciate storage class for ConfigDict: " + annotation.cls(), e);
		}
	}

	public void load(final YamlConfiguration yaml) {
		try {
			field.set(owner, load_from_yaml(yaml));
		} catch (final IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
