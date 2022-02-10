package org.oddlama.vane.core.itemv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.itemv2.api.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public class VaneCustomItem<T extends Module<T>> extends Listener<T> implements CustomItem {
	private VaneItem annotation = getClass().getAnnotation(VaneItem.class);

	public NamespacedKey key;
	public Material baseMaterial;
	public int customModelData;
	public Component displayName;

	public VaneCustomItem(Context<T> context) {
		super(null);
		// Set namespace delayed, as we need to access instance methods to do so.
		context = context.group("item_" + name(), "Enable item " + name());
		set_context(context);

		recipes = new RecipeConfig(this, context).default_recipes(this::default_recipes);
	}

	public final String name() {
		return annotation.name();
	}

	@Override
	public NamespacedKey key() {
		return key;
	}

	@Override
	public final boolean enabled() {
		// parent.enabled() is the base custom-item.
		// super.enabled() is the actual item variant config setting.
		// variant.enabled() is the developer override (→ mostly just true).
		return super.enabled();
	}

	@Override
	public int version() {
		return 0;
	}

	@Override
	public Material baseMaterial() {
		return baseMaterial;
	}

	@Override
	public int customModelData() {
		return customModelData;
	}

	@Override
	public Component displayName() {
		return displayName;
	}

	@Override
	public @Nullable TranslatableComponent durabilityLore() {
		return null;
	}

	@Override
	public int durability() {
		return 0;
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

	public void add_default_recipes() {}

	@Override
	public void on_config_change() {
		// Recipes are processed in on_config_change and not in on_disable() / on_enable(),
		// as they could change even if an item is new disabled but the plugin is still
		// enabled and was reloaded.
		recipes.deregister();
		if (enabled()) {
			recipes.register();
		}
	}
}
