package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.ItemUtil.translate_item;
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
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.ResourcePackTranslation;
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

	@LangString
	public String lang_name;

	@LangString
	public String lang_lore;

	public TranslatedItemStack(Context<T> context, Material def_material, int def_amount, String desc) {
		super(null);
	}

	public ItemStack item() {
		return new ItemStack(config_material, config_amount);
		// TODO return translate_item(new ItemStack(config_material, config_amount), "item." + lang_name_translation_namespace() + "." + lang_name_translation_key());
	}

	@Override
	public void on_enable() {}
	@Override
	public void on_disable() {}
}

