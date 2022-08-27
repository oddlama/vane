package org.oddlama.vane.trifles.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

import net.kyori.adventure.text.Component;

@VaneItem(name = "home_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 25, model_data = 0x760000, version = 1)
public class HomeScroll extends Scroll {
	public HomeScroll(Context<Trifles> context) {
		super(context, 10000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("abc", "epe")
			.set_ingredient('p', "vane_trifles:papyrus_scroll")
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('a', Material.CAMPFIRE)
			.set_ingredient('b', Material.GOAT_HORN)
			.set_ingredient('c', Tag.BEDS)
			.result(key().toString()));
	}

	//@Override
	//public LootTableList default_loot_tables() {
	//	// TODO spawn scroll with 1 usage! possible with nbt nice.
	//}

	@Override
	public Location teleport_location(final ItemStack scroll, Player player, boolean imminent_teleport) {
		final var to_location = player.getBedSpawnLocation();
		if (imminent_teleport && to_location == null) {
			final var to_potential_location = player.getPotentialBedLocation();
			if (to_potential_location != null) {
				// "You have no home bed or charged respawn anchor, or it was obstructed"
				// The most cursed sentence in minecraft.
				player.sendActionBar(Component.translatable("block.minecraft.spawn.not_valid"));
			} else {
				// "Sleep in a bed to change your respawn point"
				player.sendActionBar(Component.translatable("advancements.adventure.sleep_in_bed.description"));
			}
		}
		return to_location;
	}
}
