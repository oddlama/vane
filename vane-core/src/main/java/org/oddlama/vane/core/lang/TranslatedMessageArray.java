package org.oddlama.vane.core.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.core.module.Module;

public class TranslatedMessageArray {

    private Module<?> module;
    private String key;
    private List<String> default_translation;

    public TranslatedMessageArray(final Module<?> module, final String key, final List<String> default_translation) {
        this.module = module;
        this.key = key;
        this.default_translation = default_translation;
    }

    public int size() {
        return default_translation.size();
    }

    public String key() {
        return key;
    }

    public List<String> str(Object... args) {
        try {
            final var args_as_strings = new Object[args.length];
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Component) {
                    args_as_strings[i] = LegacyComponentSerializer.legacySection().serialize((Component) args[i]);
                } else if (args[i] instanceof String) {
                    args_as_strings[i] = args[i];
                } else {
                    throw new RuntimeException(
                        "Error while formatting message '" +
                        key() +
                        "', invalid argument to str() serializer: " +
                        args[i]
                    );
                }
            }

            final var list = new ArrayList<String>();
            for (final var s : default_translation) {
                list.add(String.format(s, args_as_strings));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error while formatting message '" + key() + "'", e);
        }
    }

    public List<Component> format(Object... args) {
        if (!module.core.config_client_side_translations) {
            return str(args)
                .stream()
                .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                .collect(Collectors.toList());
        }

        final var arr = new ArrayList<Component>();
        for (int i = 0; i < default_translation.size(); ++i) {
            final var list = new ArrayList<ComponentLike>();
            for (final var o : args) {
                if (o instanceof ComponentLike) {
                    list.add((ComponentLike) o);
                } else if (o instanceof String) {
                    list.add(LegacyComponentSerializer.legacySection().deserialize((String) o));
                } else {
                    throw new RuntimeException(
                        "Error while formatting message '" + key() + "', got invalid argument " + o
                    );
                }
            }
            arr.add(Component.translatable(key + "." + i, list));
        }
        return arr;
    }
}
