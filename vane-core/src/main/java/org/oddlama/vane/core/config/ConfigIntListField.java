package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigIntListField extends ConfigField<List<Integer>> {

    public ConfigIntList annotation;

    public ConfigIntListField(Object owner, Field field, Function<String, String> map_name, ConfigIntList annotation) {
        super(owner, field, map_name, "int list", annotation.desc());
        this.annotation = annotation;
    }

    private void append_int_list_definition(StringBuilder builder, String indent, String prefix, List<Integer> def) {
        append_list_definition(builder, indent, prefix, def, (b, i) -> b.append(i));
    }

    @Override
    public List<Integer> def() {
        final var override = overridden_def();
        if (override != null) {
            return override;
        } else {
            return Arrays.asList(ArrayUtils.toObject(annotation.def()));
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
        append_value_range(builder, indent, annotation.min(), annotation.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        append_int_list_definition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
            ? load_from_yaml(existing_compatible_config)
            : def();
        append_int_list_definition(builder, indent, "", def);
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isList(yaml_path())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected list");
        }

        for (var obj : yaml.getList(yaml_path())) {
            if (!(obj instanceof Number)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected int");
            }

            var val = yaml.getInt(yaml_path());
            if (annotation.min() != Integer.MIN_VALUE && val < annotation.min()) {
                throw new YamlLoadException(
                    "Configuration '" + yaml_path() + "' has an invalid value: Value must be >= " + annotation.min()
                );
            }
            if (annotation.max() != Integer.MAX_VALUE && val > annotation.max()) {
                throw new YamlLoadException(
                    "Configuration '" + yaml_path() + "' has an invalid value: Value must be <= " + annotation.max()
                );
            }
        }
    }

    public List<Integer> load_from_yaml(YamlConfiguration yaml) {
        final var list = new ArrayList<Integer>();
        for (var obj : yaml.getList(yaml_path())) {
            list.add(((Number) obj).intValue());
        }
        return list;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
