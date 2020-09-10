package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.Util.namespaced_key;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigItemStackField extends ConfigField<ItemStack> {
	public ConfigItemStack annotation;

	public ConfigItemStackField(Object owner, Field field, Function<String, String> map_name, ConfigItemStack annotation) {
		super(owner, field, map_name, "item stack", annotation.desc());
		this.annotation = annotation;
	}

	private void append_item_stack_definition(StringBuilder builder, String indent, String prefix) {
		final var item = def();

		// Material
		builder.append(indent);
		builder.append(prefix);
		builder.append("  material: ");
		final var material = "\"" + escape_yaml(item.getType().getKey().getNamespace()) + ":" + escape_yaml(item.getType().getKey().getKey()) + "\"";
		builder.append(material);
		builder.append("\n");

		// Amount
		if (item.getAmount() != 1) {
			builder.append(indent);
			builder.append(prefix);
			builder.append("  amount: ");
			builder.append(item.getAmount());
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
	public void generate_yaml(StringBuilder builder, String indent) {
		append_description(builder, indent);

		// Default
		builder.append(indent);
		builder.append("# Default:\n");
		append_item_stack_definition(builder, indent, "# ");

		// Definition
		builder.append(indent);
		builder.append(basename());
		builder.append(":\n");
		append_item_stack_definition(builder, indent, "");
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
						throw new YamlLoadException("Invalid material for yaml path '" + yaml_path() + "': '" + str + "' is not a valid namespaced key");
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

	public void load(YamlConfiguration yaml) {
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
		final var item = new ItemStack(material, amount);

		try {
			field.set(owner, item);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

