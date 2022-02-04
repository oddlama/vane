package org.oddlama.vane.core.item;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Maps Minecraft ResourceLocations to Models.
 * Assumes CustomModelData is an int lookup, and not the floaty predicate bullshit it really is.
 */
public abstract class ResourcePackModelRegistry {

	private static final String PREDICATE = "predicate";
	private static final String OVERRIDES = "overrides";
	private static final String CUSTOM_MODEL_DATA = "custom_model_data";

	public void scan(URI resource_pack) throws IOException {
		try (var zip = FileSystems.newFileSystem(resource_pack, Map.of())) {
			// No modded support, so only scan minecraft at this stage.
			var itemDir = zip.getPath("assets", "minecraft", "models", "item");
			final Map<NamespacedKey, item_resource_predicate> item_definitions = Files
				.walk(itemDir, 0)
				.filter(Files::isRegularFile)
				.flatMap(p -> {
					final File file = p.toFile();
					try {
						//noinspection UnstableApiUsage
						return Stream.of(
							new item_contents_and_title(
								NamespacedKey.minecraft(
									com.google.common.io.Files.getNameWithoutExtension(file.getName())
								),
								new JSONObject(Files.readString(p))
							)
						);
					} catch (IOException e) {
						return Stream.empty();
					}
				})
				.flatMap(this::get_model_keys_from)
				.collect(Collectors.toUnmodifiableMap(item_resource_predicate::model, Function.identity()));
		}
	}

	Stream<item_resource_predicate> get_model_keys_from(item_contents_and_title item) {
		if (!item.contents().has(OVERRIDES)) return Stream.empty();
		final JSONArray overrides = item.contents().getJSONArray(OVERRIDES);
		return overrides
			.toList()
			.stream()
			.map(JSONObject.class::cast)
			.flatMap(o -> {
				final JSONObject predicate = o.getJSONObject(PREDICATE);
				final String ignored = predicate
					.keySet()
					.stream()
					.filter(k -> !CUSTOM_MODEL_DATA.equals(k))
					.collect(Collectors.joining(", "));
				if (!predicate.has(CUSTOM_MODEL_DATA)) return Stream.empty();
				return Stream.of(
					new item_resource_predicate(
						item.item(),
						predicate.getInt(CUSTOM_MODEL_DATA),
						NamespacedKey.fromString(o.getString("model")),
						predicate
					)
				);
			});
	}
}

record item_contents_and_title(NamespacedKey item, JSONObject contents) {}

record item_resource_predicate(NamespacedKey item, int custom_model_data, NamespacedKey model, JSONObject predicate) {
	// Complete list of **serializable** predicates as of 1.18.1
	private static final String DAMAGE = "damage";
	private static final String DAMAGED = "damaged";
	private static final String FIREWORK = "firework";
	private static final String CHARGED = "charged";
	private static final String CUSTOM_MODEL_DATA = "custom_model_data";

	ItemStack as_item_stack() {
		final Material material = Material.valueOf(item.value());
		final ItemStack itemStack = new ItemStack(material);
		final ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setCustomModelData(custom_model_data);
		if (itemMeta instanceof final Damageable dmg) {
			if (predicate.has(DAMAGE)) {
				final short maxDurability = material.getMaxDurability();
				dmg.setDamage((int) (predicate.getFloat(DAMAGE) * maxDurability));
			} else if (predicate.has(DAMAGED)) {
				if (predicate.getInt(DAMAGED) == 1) {
					final short maxDurability = material.getMaxDurability();
					dmg.setDamage(maxDurability - 1);
				} // Assume undamaged just works.
			}
		}
		if (itemMeta instanceof final CrossbowMeta cb) {
			if (predicate.has(FIREWORK)) {
				final ItemStack fw = new ItemStack(Material.FIREWORK_ROCKET);
				cb.addChargedProjectile(fw);
			} else if (predicate.has(CHARGED)) {
				final ItemStack arrow = new ItemStack(Material.ARROW);
				cb.addChargedProjectile(arrow);
			}
		}
		itemMeta.displayName(Component.text(model.asString()));
		final Map<String, Object> props = predicate.toMap();
		final List<Component> lore = props
			.entrySet()
			.stream()
			.map(e ->
				Component
					.text()
					.content(e.getKey())
					.color(NamedTextColor.BLUE)
					.append(Component.text().content(" : ").color(NamedTextColor.WHITE))
					.append(Component.text().content(e.getValue().toString()).color(NamedTextColor.AQUA))
					.build()
			)
			.collect(Collectors.toList());
		itemMeta.lore(lore);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}
}
