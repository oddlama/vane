package org.oddlama.vane.trifles.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.commands.Setspawn;

@VaneItem(name = "spawn_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 40, model_data = 0x760010, version = 1)
public class SpawnScroll extends Scroll {
	public SpawnScroll(Context<Trifles> context) {
		super(context, 6000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("aba", "epe")
			.set_ingredient('p', "vane_trifles:papyrus_scroll")
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('a', Material.WHEAT_SEEDS)
			.set_ingredient('b', Tag.SAPLINGS)
			.result(key().toString()));
	}

	@Override
	public Location teleport_location(final ItemStack scroll, Player player, boolean imminent_teleport) {
		Location loc = null;
		for (final var world : get_module().getServer().getWorlds()) {
			if (world.getPersistentDataContainer().getOrDefault(Setspawn.IS_SPAWN_WORLD, PersistentDataType.INTEGER, 0) == 1) {
				loc = world.getSpawnLocation();
			}
		}
		// Fallback to spawn location of the first world
		if (loc == null) {
			loc = get_module().getServer().getWorlds().get(0).getSpawnLocation();
		}
		return loc;
	}
}
