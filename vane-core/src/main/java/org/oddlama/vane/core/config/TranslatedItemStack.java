package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.ItemUtil.name_item;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.lang.TranslatedMessageArray;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

import net.kyori.adventure.text.Component;

public class TranslatedItemStack<T extends Module<T>> extends ModuleComponent<T> {

	@ConfigInt(def = 1, min = 0, desc = "The item stack amount.")
	public int config_amount;

	@ConfigExtendedMaterial(
		def = "minecraft:barrier",
		desc = "The item stack material. Also accepts heads from the head library or from defined custom items."
	)
	public ExtendedMaterial config_material;

	@LangMessage
	public TranslatedMessage lang_name;

	@LangMessageArray
	public TranslatedMessageArray lang_lore;

	private ExtendedMaterial def_material;
	private int def_amount;

	public TranslatedItemStack(
		final Context<T> context,
		final String config_namespace,
		final NamespacedKey def_material,
		int def_amount,
		final String desc
	) {
		this(context, config_namespace, ExtendedMaterial.from(def_material), def_amount, desc);
	}

	public TranslatedItemStack(
		final Context<T> context,
		final String config_namespace,
		final Material def_material,
		int def_amount,
		final String desc
	) {
		this(context, config_namespace, ExtendedMaterial.from(def_material), def_amount, desc);
	}

	public TranslatedItemStack(
		final Context<T> context,
		final String config_namespace,
		final ExtendedMaterial def_material,
		int def_amount,
		final String desc
	) {
		super(context.namespace(config_namespace, desc));
		this.def_material = def_material;
		this.def_amount = def_amount;
	}

	public ItemStack item(Object... args) {
		return name_item(config_material.item(config_amount), lang_name.format(args), lang_lore.format(args));
	}

	public ItemStack item_transform_lore(Consumer<List<Component>> f_lore, Object... args) {
		final var lore = lang_lore.format(args);
		f_lore.accept(lore);
		return name_item(config_material.item(config_amount), lang_name.format(args), lore);
	}

	public ItemStack item_amount(int amount, Object... args) {
		return name_item(config_material.item(amount), lang_name.format(args), lang_lore.format(args));
	}

	public ItemStack alternative(final ItemStack alternative, Object... args) {
		return name_item(alternative, lang_name.format(args), lang_lore.format(args));
	}

	public ExtendedMaterial config_material_def() {
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
