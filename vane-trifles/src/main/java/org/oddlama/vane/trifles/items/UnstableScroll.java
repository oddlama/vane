package org.oddlama.vane.trifles.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.oddlama.vane.util.Util;

@VaneItem(name = "unstable_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 25, model_data = 0x760001, version = 1)
public class UnstableScroll extends Scroll {
	public static final NamespacedKey LAST_SCROLL_TELEPORT_LOCATION = Util.namespaced_key("vane", "last_scroll_teleport_location");

	public UnstableScroll(Context<Trifles> context) {
		super(context, 6000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("pip", "cbe", "plp")
			.set_ingredient('b', Material.NETHERITE_SCRAP)
			.set_ingredient('p', Material.MAP)
			.set_ingredient('i', Material.CHORUS_FRUIT)
			.set_ingredient('c', Material.COMPASS)
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('l', Material.CLOCK)
			.result(key().toString()));
	}

	@Override
	public Location teleport_location(Player player, boolean imminent_teleport) {
		return Util.storage_get_location(player.getPersistentDataContainer(), LAST_SCROLL_TELEPORT_LOCATION, null);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_teleport_scroll(final PlayerTeleportScrollEvent event) {
		Util.storage_set_location(event.getPlayer().getPersistentDataContainer(), LAST_SCROLL_TELEPORT_LOCATION, event.getFrom());
	}
}
