package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangVersionField extends LangField<Long> {

    public LangVersion annotation;

    public LangVersionField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> map_name,
        LangVersion annotation
    ) {
        super(module, owner, field, map_name);
        this.annotation = annotation;
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!(yaml.get(yaml_path()) instanceof Number)) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yaml_path() + "', expected long", this);
        }

        var val = yaml.getLong(yaml_path());
        if (val < 1) {
            throw new YamlLoadException.Lang(
                "Entry '" + yaml_path() + "' has an invalid value: Value must be >= 1",
                this
            );
        }
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.setLong(owner, yaml.getLong(yaml_path()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void add_translations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String lang_code)
        throws YamlLoadException {}
}
