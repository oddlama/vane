package org.oddlama.vane.trifles.items;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.key.Key;

@VaneItem(name = "north_compass", base = Material.COMPASS, model_data = 0x760013, version = 1)
public class NorthCompass extends CustomItem<Trifles> {

	public NorthCompass(final Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
				.shape(" m ", "mrm", " m ")
				.set_ingredient('m', Material.COPPER_INGOT)
				.set_ingredient('r', Material.REDSTONE)
				.result(key().toString()));
	}

	@Override
	public ItemStack updateItemStack(final ItemStack item_stack) {
		final var worlds = get_module().getServer().getWorlds();
		if (worlds.size() > 0) {
			final var world = worlds.get(0);
			if (world != null) {
				item_stack.editMeta(CompassMeta.class, meta -> {
					meta.setLodestone(new Location(world, 0.0, 0.0, -300000000.0));
					meta.setLodestoneTracked(false);
				});
			}
		}
		return item_stack;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_click_inventory(final InventoryClickEvent event) {
		final var item = event.getCurrentItem();
		if (item == null || item.getType() != Material.COMPASS) {
			return;
		}

		final var custom_item = get_module().core.item_registry().get(item);
		if (!(custom_item instanceof NorthCompass north_compass) || !north_compass.enabled()) {
			return;
		}

		// FIXME: not very performant to do this on every click, but
		// there aren't many options if we want to support other plugins creating
		// this item. (e.g. to allow giving it to players in kits, shops, ...)
		item.editMeta(CompassMeta.class, meta -> {
			// Only if it isn't already initialized. This allows making different
			// compasses for different worlds. The world in which it is crafted
			// is stored forever.
			if (!meta.hasLodestone()) {
				meta.setLodestoneTracked(false);
				meta.setLodestone(new Location(event.getWhoClicked().getWorld(), 0.0, 0.0, -300000000.0));
			}
		});
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
	}

	@Override
	public void addResources(final ResourcePackGenerator rp) throws IOException {
		final var angle_item_overrides = List.of(
			Pair.of(0.000000f, 16),
			Pair.of(0.015625f, 17),
			Pair.of(0.046875f, 18),
			Pair.of(0.078125f, 19),
			Pair.of(0.109375f, 20),
			Pair.of(0.140625f, 21),
			Pair.of(0.171875f, 22),
			Pair.of(0.203125f, 23),
			Pair.of(0.234375f, 24),
			Pair.of(0.265625f, 25),
			Pair.of(0.296875f, 26),
			Pair.of(0.328125f, 27),
			Pair.of(0.359375f, 28),
			Pair.of(0.390625f, 29),
			Pair.of(0.421875f, 30),
			Pair.of(0.453125f, 31),
			Pair.of(0.484375f, 0),
			Pair.of(0.515625f, 1),
			Pair.of(0.546875f, 2),
			Pair.of(0.578125f, 3),
			Pair.of(0.609375f, 4),
			Pair.of(0.640625f, 5),
			Pair.of(0.671875f, 6),
			Pair.of(0.703125f, 7),
			Pair.of(0.734375f, 8),
			Pair.of(0.765625f, 9),
			Pair.of(0.796875f, 10),
			Pair.of(0.828125f, 11),
			Pair.of(0.859375f, 12),
			Pair.of(0.890625f, 13),
			Pair.of(0.921875f, 14),
			Pair.of(0.953125f, 15),
			Pair.of(0.984375f, 16));

		// Include standard overrides for the normal compass
		final var base_key = baseMaterial().getKey();
		for (final var angle_item : angle_item_overrides) {
			final var angle = angle_item.getLeft();
			final var num = angle_item.getRight();
			final var key_num = num == 16 ? base_key : StorageUtil.namespaced_key(base_key.namespace(), String.format("%s_%02d", base_key.value(), num));
			rp.add_item_override(base_key, key_num, predicate -> predicate.put("angle", angle));
		}
		for (final var angle_item : angle_item_overrides) {
			final var angle = angle_item.getLeft();
			final var num = angle_item.getRight();
			final var resource_name = String.format("items/%s_%02d.png", key().value(), num);
			final var resource = get_module().getResource(resource_name);
			if (resource == null) {
				throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
			}
			final var key_num = StorageUtil.namespaced_key(key().namespace(), String.format("%s_%02d", key().value(), num));
			rp.add_item_model(key_num, resource, Key.key(Key.MINECRAFT_NAMESPACE, "item/generated"));
			rp.add_item_override(base_key, key_num, predicate -> {
				predicate.put("custom_model_data", customModelData());
				predicate.put("angle", angle);
			});
		}
	}
}
