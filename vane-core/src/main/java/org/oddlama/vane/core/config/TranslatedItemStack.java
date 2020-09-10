package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.ItemUtil.name_item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class TranslatedItemStack<T extends Module<T>> extends ModuleComponent<T> {
	@ConfigInt(def = 1, min = 0, desc = "The item stack amount.")
	public int config_amount;
	@ConfigMaterial(def = Material.BARRIER, desc = "The item stack material.")
	public Material config_material;

	@LangMessage public TranslatedMessage lang_name;
	@LangMessage public TranslatedMessage lang_lore;

	private Material def_material;
	private int def_amount;

	public TranslatedItemStack(Context<T> context, String config_namespace, Material def_material, int def_amount, String desc) {
		super(context.namespace(config_namespace, desc));
		this.def_material = def_material;
		this.def_amount = def_amount;
	}

	public ItemStack item() {
		return name_item(new ItemStack(config_material, config_amount), lang_name.format(), lang_lore.format());
	}

	public Material config_material_def() {
		return def_material;
	}

	public int config_amount_def() {
		return def_amount;
	}

	@Override
	public void on_enable() {}
	@Override
	public void on_disable() {}
}

