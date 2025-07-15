package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigItemStackField extends ConfigField<ItemStack> {

    public ConfigItemStack annotation;

    public ConfigItemStackField(
        Object owner,
        Field field,
        Function<String, String> map_name,
        ConfigItemStack annotation
    ) {
        super(owner, field, map_name, "item stack", annotation.desc());
        this.annotation = annotation;
    }

    private void append_item_stack_definition(StringBuilder builder, String indent, String prefix, ItemStack def) {
        // Material
        builder.append(indent);
        builder.append(prefix);
        builder.append("  material: ");
        final var material =
            "\"" +
            escape_yaml(def.getType().getKey().getNamespace()) +
            ":" +
            escape_yaml(def.getType().getKey().getKey()) +
            "\"";
        builder.append(material);
        builder.append("\n");

        // Amount
        if (def.getAmount() != 1) {
            builder.append(indent);
            builder.append(prefix);
            builder.append("  amount: ");
            builder.append(def.getAmount());
            builder.append("\n");
        }
    }

    @Override
    public ItemStack def() {
        final var override = overridden_def();
        if (override != null) {
            return override;
        } else {
            return new ItemStack(annotation.def().type(), annotation.def().amount());
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
        append_item_stack_definition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
            ? load_from_yaml(existing_compatible_config)
            : def();
        append_item_stack_definition(builder, indent, "", def);
    }

    @Override
    public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
        check_yaml_path(yaml);

        if (!yaml.isConfigurationSection(yaml_path())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected group");
        }

        for (var var_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var var_path = yaml_path() + "." + var_key;
            switch (var_key) {
                case "material": {
                    if (!yaml.isString(var_path)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + var_path + "', expected list");
                    }

                    final var str = yaml.getString(var_path);
                    final var split = str.split(":");
                    if (split.length != 2) {
                        throw new YamlLoadException(
                            "Invalid material for yaml path '" +
                            yaml_path() +
                            "': '" +
                            str +
                            "' is not a valid namespaced key"
                        );
                    }
                    break;
                }
                case "amount": {
                    if (!(yaml.get(var_path) instanceof Number)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected int");
                    }
                    final var val = yaml.getInt(yaml_path());
                    if (val < 0) {
                        throw new YamlLoadException("Invalid value for yaml path '" + yaml_path() + "' Must be >= 0");
                    }
                    break;
                }
            }
        }
    }

    public ItemStack load_from_yaml(YamlConfiguration yaml) {
        var material_str = "";
        var amount = 1;
        for (var var_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
            final var var_path = yaml_path() + "." + var_key;
            switch (var_key) {
                case "material": {
                    amount = 0;
                    material_str = yaml.getString(var_path);
                    break;
                }
                case "amount": {
                    amount = yaml.getInt(var_path);
                    break;
                }
            }
        }

        final var split = material_str.split(":");
        final var material = material_from(namespaced_key(split[0], split[1]));
        return new ItemStack(material, amount);
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, load_from_yaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
