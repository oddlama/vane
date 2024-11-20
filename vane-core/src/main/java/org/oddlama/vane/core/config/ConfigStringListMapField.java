package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigStringListMapEntry;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringListMapField extends ConfigField<Map<String, List<String>>> {

    public ConfigStringListMap annotation;

    public ConfigStringListMapField(
        Object owner,
        Field field,
        Function<String, String> map_name,
        ConfigStringListMap annotation
    ) {
        super(owner, field, map_name, "map of string to string list", annotation.desc());
        this.annotation = annotation;
    }

    private void append_string_list_map_definition(
        StringBuilder builder,
        String indent,
        String prefix,
        Map<String, List<String>> def
    ) {
        def.forEach((k, list) -> {
            builder.append(indent);
            builder.append(prefix);
            builder.append("  ");
            builder.append(escape_yaml(k));
            builder.append(":\n");

            list.forEach(s -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("    - ");
                builder.append(escape_yaml(s));
                builder.append("\n");
            });
        });
    }

    @Override
    public Map<String, List<String>> def() {
        final var override = overridden_def();
        if (override != null) {
            return override;
        } else {
            return Arrays.stream(annotation.def()).collect(
                Collectors.toMap(ConfigStringListMapEntry::key, e -> Arrays.asList(e.list()))
            );
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

    @Override
    public void generate_yaml(StringBuilder builder, String indent, YamlConfiguration existing_compatible_config) {
        append_description(builder, indent);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        append_string_list_map_definition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
            ? load_from_yaml(existing_compatible_config)
            : def();
        append_string_list_map_definition(builder, indent, "", def);
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isConfigurationSection(yaml_path())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected group");
        }

        for (var list_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var list_path = yaml_path() + "." + list_key;
            if (!yaml.isList(list_path)) {
                throw new YamlLoadException("Invalid type for yaml path '" + list_path + "', expected list");
            }

            for (var obj : yaml.getList(list_path)) {
                if (!(obj instanceof String)) {
                    throw new YamlLoadException("Invalid type for yaml path '" + list_path + "', expected string");
                }
            }
        }
    }

    public Map<String, List<String>> load_from_yaml(YamlConfiguration yaml) {
        final var map = new HashMap<String, List<String>>();
        for (final var list_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var list_path = yaml_path() + "." + list_key;
            final var list = new ArrayList<String>();
            map.put(list_key, list);
            for (final var obj : yaml.getList(list_path)) {
                list.add((String) obj);
            }
        }
        return map;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
