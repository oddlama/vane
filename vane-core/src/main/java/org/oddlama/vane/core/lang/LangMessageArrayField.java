package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangMessageArrayField extends LangField<TranslatedMessageArray> {

    public LangMessageArray annotation;

    public LangMessageArrayField(
        Module<?> module,
        Object owner,
        Field field,
        Function<String, String> map_name,
        LangMessageArray annotation
    ) {
        super(module, owner, field, map_name);
        this.annotation = annotation;
    }

    @Override
    public void check_loadable(final YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isList(yaml_path())) {
            throw new YamlLoadException.Lang("Invalid type for yaml path '" + yaml_path() + "', expected list", this);
        }

        for (final var obj : yaml.getList(yaml_path())) {
            if (!(obj instanceof String)) {
                throw new YamlLoadException.Lang(
                    "Invalid type for yaml path '" + yaml_path() + "', expected string",
                    this
                );
            }
        }
    }

    private List<String> from_yaml(final YamlConfiguration yaml) {
        final var list = new ArrayList<String>();
        for (final var obj : yaml.getList(yaml_path())) {
            list.add((String) obj);
        }
        return list;
    }

    @Override
    public void load(final String namespace, final YamlConfiguration yaml) {
        try {
            field.set(owner, new TranslatedMessageArray(module(), key(), from_yaml(yaml)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    @Override
    public void add_translations(final ResourcePackGenerator pack, final YamlConfiguration yaml, String lang_code)
        throws YamlLoadException {
        check_loadable(yaml);
        final var list = from_yaml(yaml);
        final var loaded_size = get().size();
        if (list.size() != loaded_size) {
            throw new YamlLoadException.Lang(
                "All translation lists for message arrays must have the exact same size. The loaded language file has " +
                loaded_size +
                " entries, while the currently processed file has " +
                list.size(),
                this
            );
        }
        for (int i = 0; i < list.size(); ++i) {
            pack.translations(namespace(), lang_code).put(key() + "." + i, list.get(i));
        }
    }
}
