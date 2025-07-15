package org.oddlama.vane.core.lang;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

public class LangManager {

    Module<?> module;
    private List<LangField<?>> lang_fields = new ArrayList<>();
    LangVersionField field_version;

    public LangManager(Module<?> module) {
        this.module = module;
        compile(module, s -> s);
    }

    public long expected_version() {
        return module.annotation.lang_version();
    }

    private boolean has_lang_annotation(Field field) {
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                return true;
            }
        }
        return false;
    }

    private void assert_field_prefix(Field field) {
        if (!field.getName().startsWith("lang_")) {
            throw new RuntimeException("Language fields must be prefixed lang_. This is a bug.");
        }
    }

    private LangField<?> compile_field(Object owner, Field field, Function<String, String> map_name) {
        assert_field_prefix(field);

        // Get the annotation
        Annotation annotation = null;
        for (var a : field.getAnnotations()) {
            if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                if (annotation == null) {
                    annotation = a;
                } else {
                    throw new RuntimeException("Language fields must have exactly one @Lang annotation.");
                }
            }
        }
        assert annotation != null;
        final var atype = annotation.annotationType();

        // Return a correct wrapper object
        if (atype.equals(LangMessage.class)) {
            return new LangMessageField(module, owner, field, map_name, (LangMessage) annotation);
        } else if (atype.equals(LangMessageArray.class)) {
            return new LangMessageArrayField(module, owner, field, map_name, (LangMessageArray) annotation);
        } else if (atype.equals(LangVersion.class)) {
            if (owner != module) {
                throw new RuntimeException("@LangVersion can only be used inside the main module. This is a bug.");
            }
            if (field_version != null) {
                throw new RuntimeException(
                    "There must be exactly one @LangVersion field! (found multiple). This is a bug."
                );
            }
            return field_version = new LangVersionField(module, owner, field, map_name, (LangVersion) annotation);
        } else {
            throw new RuntimeException("Missing LangField handler for @" + atype.getName() + ". This is a bug.");
        }
    }

    private boolean verify_version(File file, long version) {
        if (version != expected_version()) {
            module.log.severe(file.getName() + ": expected version " + expected_version() + ", but got " + version);

            if (version == 0) {
                module.log.severe("Something went wrong while generating or loading the configuration.");
                module.log.severe("If you are sure your configuration is correct and this isn't a file");
                module.log.severe(
                    "system permission issue, please report this to https://github.com/oddlama/vane/issues"
                );
            } else if (version < expected_version()) {
                module.log.severe("This language file is for an older version of " + module.getName() + ".");
                module.log.severe("Please update your file or use an officially supported language file.");
            } else {
                module.log.severe("This language file is for a future version of " + module.getName() + ".");
                module.log.severe("Please use the correct file for this version, or use an officially");
                module.log.severe("supported language file.");
            }

            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public void compile(Object owner, Function<String, String> map_name) {
        // Compile all annotated fields
        lang_fields.addAll(
            getAllFields(owner.getClass())
                .stream()
                .filter(this::has_lang_annotation)
                .map(f -> compile_field(owner, f, map_name))
                .toList()
        );

        if (owner == module && field_version == null) {
            throw new RuntimeException("There must be exactly one @LangVersion field! (found none). This is a bug.");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get_field(String name) {
        var field = lang_fields
            .stream()
            .filter(f -> f.get_name().equals(name))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Missing lang field lang_" + name));

        try {
            return (T) field;
        } catch (ClassCastException e) {
            throw new RuntimeException("Invalid lang field type for lang_" + name, e);
        }
    }

    public boolean reload(File file) {
        // Load file
        final var yaml = YamlConfiguration.loadConfiguration(file);

        // Check version
        final var version = yaml.getLong("version", -1);
        if (!verify_version(file, version)) {
            return false;
        }

        try {
            // Check languration for errors
            for (var f : lang_fields) {
                f.check_loadable(yaml);
            }

            for (var f : lang_fields) {
                f.load(module.namespace(), yaml);
            }
        } catch (YamlLoadException e) {
            module.log.log(Level.SEVERE, "error while loading '" + file.getAbsolutePath() + "'", e);
            return false;
        }
        return true;
    }

    public void generate_resource_pack(final ResourcePackGenerator pack, YamlConfiguration yaml, File lang_file) {
        var lang_code = yaml.getString("resource_pack_lang_code");
        if (lang_code == null) {
            throw new RuntimeException("Missing yaml key: resource_pack_lang_code");
        }
        var errors = new LinkedList<YamlLoadException.Lang>();
        for (var f : lang_fields) {
            try {
                f.add_translations(pack, yaml, lang_code);
            } catch (YamlLoadException.Lang e) {
                errors.add(e);
            } catch (YamlLoadException e) {
                module.log.log(Level.SEVERE, "Unexpected YAMLLoadException: ", e);
            }
        }
        if (errors.size() > 0) {
            final String errored_lang_nodes = errors
                .stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining("\n\t\t"));
            module.log.log(
                Level.SEVERE,
                "The following errors were identified while adding translations from \n\t" +
                lang_file.getAbsolutePath() +
                " \n\t\t" +
                errored_lang_nodes
            );
        }
    }
}
