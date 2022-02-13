package org.oddlama.vane.core.itemv2;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.item.VaneItemv2;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.Recipes;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CustomItem<T extends Module<T>> extends Listener<T> implements org.oddlama.vane.core.itemv2.api.CustomItem {
	private VaneItemv2 annotation = getClass().getAnnotation(VaneItemv2.class);
	public NamespacedKey key;

	public Recipes<T> recipes;

	// Language
	@LangMessage
	public TranslatedMessage lang_name;

	public CustomItem(Context<T> context) {
		super(null);
		// Set namespace delayed, as we need to access instance methods to do so.
		context = context.group("item_" + annotation.name(), "Enable item " + annotation.name());
		set_context(context);

		this.key = Util.namespaced_key(get_module().namespace(), annotation.name());
		recipes = new Recipes<T>(get_context(), this.key, this::default_recipes);
	}

	@Override
	public NamespacedKey key() {
		return key;
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

	@Override
	public @Nullable TranslatableComponent durabilityLore() {
		return Component.translatable("item.durability")
			.color(NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false);
	}

	@Override
	public int durability() {
		return annotation.durability();
	}

	public RecipeList default_recipes() {
		return RecipeList.of();
	}

	@Override
	public void addResources() {
		//final var resource_name = "items/" + variant_name + ".png";
		//final var resource = get_module().getResource(resource_name);
		//if (resource == null) {
		//	throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
		//}
		//pack.add_item_model(key, resource);
		//pack.add_item_override(
		//	base().getKey(),
		//	key,
		//	predicate -> {
		//		predicate.put("custom_model_data", model_data());
		//	}
		//);
	}
}
