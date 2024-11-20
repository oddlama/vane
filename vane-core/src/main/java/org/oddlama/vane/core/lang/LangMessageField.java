package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangMessageField extends LangField<TranslatedMessage> {

    public LangMessage annotation;

    public LangMessageField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> map_name,
        LangMessage annotation
    ) {
        super(module, owner, field, map_name);
        this.annotation = annotation;
    }

    @Override
    public void check_loadable(final YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isString(yaml_path())) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yaml_path() + "', expected string", this);
        }
    }

    private String from_yaml(final YamlConfiguration yaml) {
        return yaml.getString(yaml_path());
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.set(owner, new TranslatedMessage(module(), key(), from_yaml(yaml)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void add_translations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String lang_code)
        throws YamlLoadException {
        check_loadable(yaml);
        pack.translations(namespace(), lang_code).put(key(), from_yaml(yaml));
    }
}
