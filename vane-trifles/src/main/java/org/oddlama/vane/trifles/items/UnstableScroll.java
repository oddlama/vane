package org.oddlama.vane.trifles.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(name = "unstable_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 25, model_data = 0x760001, version = 1)
public class UnstableScroll extends Scroll {
	public static final NamespacedKey LAST_SCROLL_TELEPORT_LOCATION = StorageUtil.namespaced_key("vane", "last_scroll_teleport_location");

	@LangMessage
	public TranslatedMessage lang_teleport_no_previous_teleport;

	public UnstableScroll(Context<Trifles> context) {
		super(context, 6000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("aba", "epe")
			.set_ingredient('p', "vane_trifles:papyrus_scroll")
			.set_ingredient('e', Material.CHORUS_FRUIT)
			.set_ingredient('a', Material.AMETHYST_SHARD)
			.set_ingredient('b', Material.COMPASS)
			.result(key().toString()));
	}

	@Override
	public Location teleport_location(final ItemStack scroll, Player player, boolean imminent_teleport) {
		final var loc = StorageUtil.storage_get_location(player.getPersistentDataContainer(), LAST_SCROLL_TELEPORT_LOCATION, null);
		if (imminent_teleport && loc == null) {
			lang_teleport_no_previous_teleport.send_action_bar(player);
		}
		return loc;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_teleport_scroll(final PlayerTeleportScrollEvent event) {
		StorageUtil.storage_set_location(event.getPlayer().getPersistentDataContainer(), LAST_SCROLL_TELEPORT_LOCATION, event.getFrom());
	}
}
