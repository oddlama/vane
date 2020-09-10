package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.Util.namespaced_key;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.annotation.config.ConfigItemStackDef;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.module.Context;

public class TranslatedItemStack<T extends Module<T>> extends ModuleComponent<T> {
	@ConfigMaterial(def = Material.BARRIER, desc = "")
	public Material config_material;

	@ConfigInt(def = 1, min = 0, desc = "")
	public int config_amount;

	@LangMessage
	public TranslatedMessage lang_name;

	@LangMessage
	public TranslatedMessage lang_lore;

	public TranslatedItemStack(Context<T> context, Material def_material, int def_amount, String desc) {
		super(null);
	}

	public ItemStack item() {
		return new ItemStack(config_material, config_amount);
		// TODO return name_item(new ItemStack(config_material, config_amount), lang_name, lang_lore);
	}

	@Override
	public void on_enable() {}
	@Override
	public void on_disable() {}
}

