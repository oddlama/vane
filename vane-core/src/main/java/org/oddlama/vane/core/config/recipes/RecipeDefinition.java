package org.oddlama.vane.core.config.recipes;

import static org.oddlama.vane.util.MaterialUtil.material_from;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.util.Util;

import net.minecraft.commands.arguments.item.ItemParser;

public abstract class RecipeDefinition {
	public String name;

	public RecipeDefinition(final String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public NamespacedKey key(final NamespacedKey base_key) {
		return Util.namespaced_key(base_key.namespace(), base_key.value() + "_" + name);
	}

	public abstract Recipe to_recipe(final NamespacedKey base_key);
	public abstract Object to_dict();
	public abstract RecipeDefinition from_dict(Object dict);

	public static RecipeDefinition from_dict(final String name, final Object dict) {
		if (!(dict instanceof Map<?,?>)) {
			throw new IllegalArgumentException("Invalid recipe dictionary: Argument must be a Map<String, Object>, but is " + dict.getClass() + "!");
		}
		final var type = ((Map<?,?>)dict).get("type");
		if (type == null || !(type instanceof String)) {
			throw new IllegalArgumentException("Invalid recipe dictionary: recipe type must exist and be a string!");
		}

		final var str_type = (String)type;
		switch (str_type) {
			case "shaped":       return new ShapedRecipeDefinition(name).from_dict(dict);
			//case "shapeless":    return new ShapelessRecipeDefinition(name).from_dict(dict);
			//case "blasting":     // fallthrough
			//case "furnace":      // fallthrough
			//case "campfire":     // fallthrough
			//case "smoking":      return new CookingRecipeDefinition(name, str_type).from_dict(dict);
			//case "smithing":     return new SmithingRecipeDefinition(name).from_dict(dict);
			//case "stonecutting": return new StonecuttingRecipeDefinition(name).from_dict(dict);
			default: break;
		}

		throw new IllegalArgumentException("Unknown recipe type '" + str_type + "'");
	}

	public static @NotNull ItemStack itemstack(final String definition) {
		// namespace:key or namespace:key{nbtdata}, where the key can reference a material, head material or customitem.
		final var nbt_delim = definition.indexOf('{');
		NamespacedKey key;
		if (nbt_delim == -1) {
			key = NamespacedKey.fromString(definition);
		} else {
			key = NamespacedKey.fromString(definition.substring(0, nbt_delim));
		}

		final var emat = ExtendedMaterial.from(key);
		if (emat == null) {
			throw new IllegalArgumentException("Invalid extended material definition: " + definition);
		}

		// First create the itemstack as if we had no NBT information.
		var item_stack = emat.item();
		if (nbt_delim == -1) {
			// There is no NBT information, we can return here.
			return item_stack;
		}

		// Parse the NBT by using minecraft's internal paerser with the base material
		// of whatever the extended material gave us.
		final var vanilla_definition = item_stack.getType().key().toString() + definition.substring(nbt_delim - 1);
		try {
			final var mojang_nbt = new ItemParser(new StringReader(vanilla_definition), false).parse().getNbt();

			System.out.println("moj: " + mojang_nbt.toString());
			System.out.println("ext: " + org.oddlama.vane.util.Nms.item_handle(item_stack).getTag().toString());
			// TODO
			// Now apply the NBT be parsed by minecraft's internal parser to the itemstack.
			return item_stack;
		} catch (CommandSyntaxException e) {
			throw new IllegalArgumentException("Could not parse NBT of item definition: " + definition, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static @NotNull RecipeChoice recipe_choice(String definition) {
		definition = definition.strip();

		// Try a material #tag
		if (definition.startsWith("#")) {
			for (final var f : Tag.class.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) && f.getType() == Tag.class) {
					try {
						final var tag = (Tag<?>)f.get(null);
						if (tag.getValues().toArray() instanceof Material[] && tag.key().toString().equals(definition.substring(1))) {
							return new RecipeChoice.MaterialChoice((Tag<Material>)tag);
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new IllegalArgumentException("Invalid material tag: " + definition);
					}
				}
			}
			throw new IllegalArgumentException("Unknown material tag: " + definition);
		}

		// Tuple of materials
		if (definition.startsWith("(") && definition.endsWith(")")) {
			final var parts = Arrays.stream(definition.substring(1, definition.length() - 1).split(","))
				.map(key -> {
					final var mat = material_from(NamespacedKey.fromString(key.strip()));
					if (mat == null) {
						throw new IllegalArgumentException("Unknown material (only normal materials are allowed in tags): " + key);
					}
					return mat;
				})
				.collect(Collectors.toList());
			return new RecipeChoice.MaterialChoice(parts);
		}

		// Exact choice of itemstack including NBT
		return new RecipeChoice.ExactChoice(itemstack(definition));
	}
}
