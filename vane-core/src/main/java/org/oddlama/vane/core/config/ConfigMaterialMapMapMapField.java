package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialMapMapMapField extends ConfigField<Map<String, Map<String, Map<String, Material>>>> {

    public ConfigMaterialMapMapMap annotation;

    public ConfigMaterialMapMapMapField(
        Object owner,
        Field field,
        Function<String, String> map_name,
        ConfigMaterialMapMapMap annotation
    ) {
        super(
            owner,
            field,
            map_name,
            "map of string to (map of string to (map of string to material))",
            annotation.desc()
        );
        this.annotation = annotation;
    }

    private void append_map_definition(
        StringBuilder builder,
        String indent,
        String prefix,
        Map<String, Map<String, Map<String, Material>>> def
    ) {
        def
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e1 -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("  ");
                builder.append(escape_yaml(e1.getKey()));
                builder.append(":\n");

                e1
                    .getValue()
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e2 -> {
                        builder.append(indent);
                        builder.append(prefix);
                        builder.append("    ");
                        builder.append(escape_yaml(e2.getKey()));
                        builder.append(":\n");

                        e2
                            .getValue()
                            .entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e3 -> {
                                builder.append(indent);
                                builder.append(prefix);
                                builder.append("      ");
                                builder.append(escape_yaml(e3.getKey()));
                                builder.append(": \"");
                                builder.append(escape_yaml(e3.getValue().getKey().getNamespace()));
                                builder.append(":");
                                builder.append(escape_yaml(e3.getValue().getKey().getKey()));
                                builder.append("\"\n");
                            });
                    });
            });
    }

    @Override
    public Map<String, Map<String, Map<String, Material>>> def() {
        final var override = overridden_def();
        if (override != null) {
            return override;
        } else {
            return Arrays.stream(annotation.def()).collect(
                Collectors.toMap(ConfigMaterialMapMapMapEntry::key, e1 ->
                    Arrays.stream(e1.value()).collect(
                        Collectors.toMap(ConfigMaterialMapMapEntry::key, e2 ->
                            Arrays.stream(e2.value()).collect(
                                Collectors.toMap(ConfigMaterialMapEntry::key, e3 -> e3.value())
                            )
                        )
                    )
                )
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
        append_map_definition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
            ? load_from_yaml(existing_compatible_config)
            : def();
        append_map_definition(builder, indent, "", def);
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isConfigurationSection(yaml_path())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected group");
        }

        for (var key1 : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var key1_path = yaml_path() + "." + key1;
            if (!yaml.isConfigurationSection(key1_path)) {
                throw new YamlLoadException("Invalid type for yaml path '" + key1_path + "', expected group");
            }

            for (var key2 : yaml.getConfigurationSection(key1_path).getKeys(false)) {
                final var key2_path = key1_path + "." + key2;
                if (!yaml.isConfigurationSection(key2_path)) {
                    throw new YamlLoadException("Invalid type for yaml path '" + key2_path + "', expected group");
                }

                for (var key3 : yaml.getConfigurationSection(key2_path).getKeys(false)) {
                    final var key3_path = key2_path + "." + key3;
                    if (!yaml.isString(key3_path)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + key3_path + "', expected string");
                    }

                    final var str = yaml.getString(key3_path);
                    final var split = str.split(":");
                    if (split.length != 2) {
                        throw new YamlLoadException(
                            "Invalid material entry in list '" +
                            key3_path +
                            "': '" +
                            str +
                            "' is not a valid namespaced key"
                        );
                    }

                    final var mat = material_from(namespaced_key(split[0], split[1]));
                    if (mat == null) {
                        throw new YamlLoadException(
                            "Invalid material entry in list '" + key3_path + "': '" + str + "' does not exist"
                        );
                    }
                }
            }
        }
    }

    public Map<String, Map<String, Map<String, Material>>> load_from_yaml(YamlConfiguration yaml) {
        final var map1 = new HashMap<String, Map<String, Map<String, Material>>>();
        for (final var key1 : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var key1_path = yaml_path() + "." + key1;
            final var map2 = new HashMap<String, Map<String, Material>>();
            map1.put(key1, map2);
            for (final var key2 : yaml.getConfigurationSection(key1_path).getKeys(false)) {
                final var key2_path = key1_path + "." + key2;
                final var map3 = new HashMap<String, Material>();
                map2.put(key2, map3);
                for (final var key3 : yaml.getConfigurationSection(key2_path).getKeys(false)) {
                    final var key3_path = key2_path + "." + key3;
                    final var split = yaml.getString(key3_path).split(":");
                    map3.put(key3, material_from(namespaced_key(split[0], split[1])));
                }
            }
        }
        return map1;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
