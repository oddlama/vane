package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialSetField extends ConfigField<Set<Material>> {

    public ConfigMaterialSet annotation;

    public ConfigMaterialSetField(
        Object owner,
        Field field,
        Function<String, String> map_name,
        ConfigMaterialSet annotation
    ) {
        super(owner, field, map_name, "set of materials", annotation.desc());
        this.annotation = annotation;
    }

    private void append_material_set_definition(
        StringBuilder builder,
        String indent,
        String prefix,
        Set<Material> def
    ) {
        append_list_definition(builder, indent, prefix, def, (b, m) -> {
            b.append("\"");
            b.append(escape_yaml(m.getKey().getNamespace()));
            b.append(":");
            b.append(escape_yaml(m.getKey().getKey()));
            b.append("\"");
        });
    }

    @Override
    public Set<Material> def() {
        final var override = overridden_def();
        if (override != null) {
            return override;
        } else {
            return new HashSet<>(Arrays.asList(annotation.def()));
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
    public void register_metrics(Metrics metrics) {
        if (!this.metrics()) return;
        metrics.addCustomChart(
            new AdvancedPie(yaml_path(), () -> {
                final var values = new HashMap<String, Integer>();
                for (final var v : get()) {
                    values.put(v.getKey().toString(), 1);
                }
                return values;
            })
        );
    }

    @Override
    public void generate_yaml(StringBuilder builder, String indent, YamlConfiguration existing_compatible_config) {
        append_description(builder, indent);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        append_material_set_definition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
            ? load_from_yaml(existing_compatible_config)
            : def();
        append_material_set_definition(builder, indent, "", def);
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isList(yaml_path())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected list");
        }

        for (var obj : yaml.getList(yaml_path())) {
            if (!(obj instanceof String)) {
                throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected string");
            }

            final var str = (String) obj;
            final var split = str.split(":");
            if (split.length != 2) {
                throw new YamlLoadException(
                    "Invalid material entry in list '" + yaml_path() + "': '" + str + "' is not a valid namespaced key"
                );
            }

            final var mat = material_from(namespaced_key(split[0], split[1]));
            if (mat == null) {
                throw new YamlLoadException(
                    "Invalid material entry in list '" + yaml_path() + "': '" + str + "' does not exist"
                );
            }
        }
    }

    public Set<Material> load_from_yaml(YamlConfiguration yaml) {
        final var set = new HashSet<Material>();
        for (var obj : yaml.getList(yaml_path())) {
            final var split = ((String) obj).split(":");
            set.add(material_from(namespaced_key(split[0], split[1])));
        }
        return set;
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
