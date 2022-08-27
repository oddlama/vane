package org.oddlama.vane.trifles.items;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(name = "death_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 2, model_data = 0x760012, version = 1)
public class DeathScroll extends Scroll {
	public static final NamespacedKey RECENT_DEATH_LOCATION = StorageUtil.namespaced_key("vane", "recent_death_location");
	public static final NamespacedKey RECENT_DEATH_TIME = StorageUtil.namespaced_key("vane", "recent_death_time");

	@LangMessage
	public TranslatedMessage lang_teleport_no_recent_death;

	public DeathScroll(Context<Trifles> context) {
		super(context, 6000);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("aba", "epe")
			.set_ingredient('p', "vane_trifles:papyrus_scroll")
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('a', Material.BONE)
			.set_ingredient('b', Material.RECOVERY_COMPASS)
			.result(key().toString()));
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		final var set = super.inhibitedBehaviors();
		// Fuck no, this will not be made unbreakable.
		set.add(InhibitBehavior.NEW_ENCHANTS);
		return set;
	}

	@Override
	public Location teleport_location(final ItemStack scroll, Player player, boolean imminent_teleport) {
		final var pdc = player.getPersistentDataContainer();
		final var time = pdc.getOrDefault(RECENT_DEATH_TIME, PersistentDataType.LONG, 0l);
		var loc = StorageUtil.storage_get_location(player.getPersistentDataContainer(), RECENT_DEATH_LOCATION, null);

		// Only recent deaths up to 20 minutes ago
		if (System.currentTimeMillis() - time > 20 * 60 * 1000l) {
			loc = null;
		}

		if (imminent_teleport) {
			if (loc == null) {
				lang_teleport_no_recent_death.send_action_bar(player);
			} else {
				// Only once
				pdc.remove(RECENT_DEATH_TIME);
				StorageUtil.storage_remove_location(pdc, RECENT_DEATH_LOCATION);
			}
		}

		return loc;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_death(final PlayerDeathEvent event) {
		final var pdc = event.getPlayer().getPersistentDataContainer();
		StorageUtil.storage_set_location(pdc, RECENT_DEATH_LOCATION, event.getPlayer().getLocation());
		pdc.set(RECENT_DEATH_TIME, PersistentDataType.LONG, System.currentTimeMillis());
	}
}
