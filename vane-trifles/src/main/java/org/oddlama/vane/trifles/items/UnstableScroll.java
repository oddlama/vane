package org.oddlama.vane.trifles.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.oddlama.vane.util.LazyLocation;

public class UnstableScroll extends Scroll {
	// Last teleport location storage
	@Persistent
	private Map<UUID, LazyLocation> storage_last_scroll_teleport = new HashMap<>();

	public UnstableScroll(Context<Trifles> context) {
		super(context, "unstable_scroll", 6000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("pip", "cbe", "plp")
			// TODO BADDDDDDDDDDDDDDDDDDDDDDDDDDDD TEST REMOVEEEEEEEEEEEEEEEEE
			.set_ingredient('b', "minecraft:stick{Enchantments:[{id:knockback,lvl:1000}]}")
			.set_ingredient('p', Material.MAP)
			.set_ingredient('i', Material.CHORUS_FRUIT)
			.set_ingredient('c', Material.COMPASS)
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('l', Material.CLOCK)
			.result(key().toString()));
	}

	@Override
	public Location teleport_location(Player player, boolean imminent_teleport) {
		if (storage_last_scroll_teleport.containsKey(player.getUniqueId())) {
			return storage_last_scroll_teleport.get(player.getUniqueId()).location();
		}
		return null;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_teleport_scroll(final PlayerTeleportScrollEvent event) {
		storage_last_scroll_teleport.put(event.getPlayer().getUniqueId(), new LazyLocation(event.getFrom()));
		mark_persistent_storage_dirty();
	}
}
