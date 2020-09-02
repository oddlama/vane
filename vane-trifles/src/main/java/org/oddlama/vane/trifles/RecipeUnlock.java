package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;

import java.util.Arrays;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerJoinEvent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.util.Nms;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class RecipeUnlock extends Listener<Trifles> {
	public RecipeUnlock(Context<Trifles> context) {
		super(context.group("recipe_unlock", "Unlocks all recipes when a player joins."));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		final var count = Nms.unlock_all_recipes(event.getPlayer());
		if (count > 0) {
			get_module().log.info("Given " + count + " recipes to " + event.getPlayer().getDisplayName());
		}
	}
}
