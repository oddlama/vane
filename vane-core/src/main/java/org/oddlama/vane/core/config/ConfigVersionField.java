package org.oddlama.vane.core.config;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;

public class ConfigVersionField extends ConfigField<Long> {

    public ConfigVersion annotation;

    public ConfigVersionField(Object owner, Field field, Function<String, String> map_name, ConfigVersion annotation) {
        super(
            owner,
            field,
            map_name,
            "version id",
            "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated."
        );
        this.annotation = annotation;

        // Version field should be at the bottom
        this.sort_priority = 100;
    }

    @Override
    public Long def() {
        return null;
    }

    @Override
    public boolean metrics() {
        return true;
    }

    @Override
    public void generate_yaml(StringBuilder builder, String indent, YamlConfiguration existing_compatible_config) {
        append_description(builder, indent);
        append_field_definition(builder, indent, ((Module<?>) owner).annotation.config_version());
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!(yaml.get(yaml_path()) instanceof Number)) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected long");
        }

        var val = yaml.getLong(yaml_path());
        if (val < 1) {
            throw new YamlLoadException("Configuration '" + yaml_path() + "' has an invalid value: Value must be >= 1");
        }
    }

    public long load_from_yaml(YamlConfiguration yaml) {
        return yaml.getLong(yaml_path());
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.setLong(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
