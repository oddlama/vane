package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.functional.Consumer2;

public abstract class ConfigField<T> implements Comparable<ConfigField<?>> {

    protected Object owner;
    protected Field field;
    protected String path;
    protected String type_name;
    protected int sort_priority = 0;

    private String[] yaml_path_components;
    private String yaml_group_path;
    private String basename;
    private Supplier<String> description;

    public ConfigField(
        Object owner,
        Field field,
        Function<String, String> map_name,
        String type_name,
        String description
    ) {
        this.owner = owner;
        this.field = field;
        this.path = map_name.apply(field.getName().substring("config_".length()));
        this.yaml_path_components = path.split("\\.");

        var last_dot = path.lastIndexOf(".");
        this.yaml_group_path = last_dot == -1 ? "" : path.substring(0, last_dot);

        this.basename = yaml_path_components[yaml_path_components.length - 1];
        this.type_name = type_name;

        // lang, enabled, metrics_enabled should be at the top
        switch (this.path) {
            case "lang" -> this.sort_priority = -10;
            case "enabled" -> this.sort_priority = -9;
            case "metrics_enabled" -> this.sort_priority = -8;
        }

        field.setAccessible(true);

        // Dynamic description
        this.description = () -> {
            try {
                return (String) owner.getClass().getMethod(field.getName() + "_desc").invoke(owner);
            } catch (NoSuchMethodException e) {
                // Ignore, field wasn't overridden
                return description;
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(
                    "Could not call " +
                    owner.getClass().getName() +
                    "." +
                    field.getName() +
                    "_desc() to override description value",
                    e
                );
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected T overridden_def() {
        try {
            return (T) owner.getClass().getMethod(field.getName() + "_def").invoke(owner);
        } catch (NoSuchMethodException e) {
            // Ignore, field wasn't overridden
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                "Could not call " +
                owner.getClass().getName() +
                "." +
                field.getName() +
                "_def() to override default value",
                e
            );
        }
    }

    protected Boolean overridden_metrics() {
        try {
            return (Boolean) owner.getClass().getMethod(field.getName() + "_metrics").invoke(owner);
        } catch (NoSuchMethodException e) {
            // Ignore, field wasn't overridden
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                "Could not call " +
                owner.getClass().getName() +
                "." +
                field.getName() +
                "_metrics() to override metrics status",
                e
            );
        }
    }

    protected String escape_yaml(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String get_yaml_group_path() {
        return path;
    }

    public String yaml_path() {
        return path;
    }

    public String yaml_group_path() {
        return yaml_group_path;
    }

    public String basename() {
        return basename;
    }

    private String modify_yaml_path_for_sorting(String path) {
        // "enable" fields should always be at the top, and therefore
        // get treated without the suffix.
        if (path.endsWith(".enabled")) {
            return path.substring(0, path.lastIndexOf(".enabled"));
        }
        return path;
    }

    @Override
    public int compareTo(ConfigField<?> other) {
        if (sort_priority != other.sort_priority) {
            return sort_priority - other.sort_priority;
        } else {
            for (int i = 0; i < Math.min(yaml_path_components.length, other.yaml_path_components.length) - 1; ++i) {
                var c = yaml_path_components[i].compareTo(other.yaml_path_components[i]);
                if (c != 0) {
                    return c;
                }
            }
            return modify_yaml_path_for_sorting(yaml_path()).compareTo(modify_yaml_path_for_sorting(other.yaml_path()));
        }
    }

    protected void append_description(StringBuilder builder, String indent) {
        final var description_wrapped =
            indent +
            "# " +
            WordUtils.wrap(description.get(), Math.max(60, 80 - indent.length()), "\n" + indent + "# ", false);
        builder.append(description_wrapped);
        builder.append("\n");
    }

    protected <U> void append_list_definition(
        StringBuilder builder,
        String indent,
        String prefix,
        Collection<U> list,
        Consumer2<StringBuilder, U> append
    ) {
        list
            .stream()
            .forEach(i -> {
                builder.append(indent);
                builder.append(prefix);
                builder.append("  - ");
                append.apply(builder, i);
                builder.append("\n");
            });
    }

    protected <U> void append_value_range(
        StringBuilder builder,
        String indent,
        U min,
        U max,
        U invalid_min,
        U invalid_max
    ) {
        builder.append(indent);
        builder.append("# Valid values: ");
        if (!min.equals(invalid_min)) {
            if (!max.equals(invalid_max)) {
                builder.append("[");
                builder.append(min);
                builder.append(",");
                builder.append(max);
                builder.append("]");
            } else {
                builder.append("[");
                builder.append(min);
                builder.append(",)");
            }
        } else {
            if (!max.equals(invalid_max)) {
                builder.append("(,");
                builder.append(max);
                builder.append("]");
            } else {
                builder.append("Any " + type_name);
            }
        }
        builder.append("\n");
    }

    protected void append_default_value(StringBuilder builder, String indent, Object def) {
        builder.append(indent);
        builder.append("# Default: ");
        builder.append(def);
        builder.append("\n");
    }

    protected void append_field_definition(StringBuilder builder, String indent, Object def) {
        builder.append(indent);
        builder.append(basename);
        builder.append(": ");
        builder.append(def);
        builder.append("\n");
    }

    protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
        if (!yaml.contains(path, true)) {
            throw new YamlLoadException("yaml is missing entry with path '" + path + "'");
        }
    }

    public abstract T def();

    // Disabled by default, fields must explicitly support this!
    public boolean metrics() {
        return false;
    }

    public abstract void generate_yaml(
        StringBuilder builder,
        String indent,
        YamlConfiguration existing_compatible_config
    );

    public abstract void check_loadable(YamlConfiguration yaml) throws YamlLoadException;

    public abstract void load(YamlConfiguration yaml);

    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    public void register_metrics(Metrics metrics) {
        if (!metrics()) return;
        metrics.addCustomChart(new SimplePie(yaml_path(), () -> get().toString()));
    }

    public String[] components() {
        return yaml_path_components;
    }

    public int group_count() {
        return yaml_path_components.length - 1;
    }

    public static boolean same_group(ConfigField<?> a, ConfigField<?> b) {
        if (a.yaml_path_components.length != b.yaml_path_components.length) {
            return false;
        }
        for (int i = 0; i < a.yaml_path_components.length - 1; ++i) {
            if (!a.yaml_path_components[i].equals(b.yaml_path_components[i])) {
                return false;
            }
        }
        return true;
    }

    public static int common_group_count(ConfigField<?> a, ConfigField<?> b) {
        int i;
        for (i = 0; i < Math.min(a.yaml_path_components.length, b.yaml_path_components.length) - 1; ++i) {
            if (!a.yaml_path_components[i].equals(b.yaml_path_components[i])) {
                return i;
            }
        }
        return i;
    }
}
