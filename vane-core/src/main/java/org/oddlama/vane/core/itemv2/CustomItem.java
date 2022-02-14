package org.oddlama.vane.core.itemv2;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItemv2;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.loot.LootTables;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.Recipes;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.util.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class CustomItem<T extends Module<T>> extends Listener<T> implements org.oddlama.vane.core.itemv2.api.CustomItem {
	private VaneItemv2 annotation = getClass().getAnnotation(VaneItemv2.class);
	public NamespacedKey key;

	public Recipes<T> recipes;
	public LootTables<T> loot_tables;

	// Language
	@LangMessage
	public TranslatedMessage lang_name;

	@ConfigInt(def = 0, min = 0, desc = "The durability of this item. Set to 0 to use the durability properties of whatever base material the item is made of.")
	private int config_durability;

	public CustomItem(Context<T> context) {
		super(null);
		// Set namespace delayed, as we need to access instance methods to do so.
		context = context.group("item_" + name(), "Enable item " + name());
		set_context(context);

		this.key = Util.namespaced_key(get_module().namespace(), name());
		recipes = new Recipes<T>(get_context(), this.key, this::default_recipes);
		loot_tables = new LootTables<T>(get_context(), this.key, this::default_loot_tables);

		// Register item
		get_module().core.item_registry().register(this);
	}

	@Override
	public NamespacedKey key() {
		return key;
	}

	public String name() {
		return annotation.name();
	}

	@Override
	public boolean enabled() {
		// Explicitly stated to not be forgotten, as enabled() is also part of Listener<T>.
		return annotation.enabled() && super.enabled();
	}

	@Override
	public int version() {
		return annotation.version();
	}

	@Override
	public Material baseMaterial() {
		return annotation.base();
	}

	@Override
	public int customModelData() {
		return annotation.model_data();
	}

	@Override
	public Component displayName() {
		return lang_name.format().decoration(TextDecoration.ITALIC, false);
	}

	public int config_durability_def() {
		return annotation.durability();
	}

	@Override
	public int durability() {
		return config_durability;
	}

	public RecipeList default_recipes() {
		return RecipeList.of();
	}

	public LootTableList default_loot_tables() {
		return LootTableList.of();
	}

	@Override
	public void addResources(final ResourcePackGenerator rp) throws IOException {
		final var resource_name = "items/" + key.value() + ".png";
		final var resource = get_module().getResource(resource_name);
		if (resource == null) {
			throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
		}
		rp.add_item_model(key, resource);
		rp.add_item_override(baseMaterial().getKey(), key, predicate -> {
			predicate.put("custom_model_data", customModelData());
		});
	}
}
