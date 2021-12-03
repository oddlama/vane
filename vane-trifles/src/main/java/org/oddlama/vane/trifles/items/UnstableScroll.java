package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.Util.ms_to_ticks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.BookVariant;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.oddlama.vane.util.LazyLocation;

@VaneItem(name = "unstable_scroll")
public class UnstableScroll extends CustomItem<Trifles, UnstableScroll> {

	// Last teleport location storage
	@Persistent
	private Map<UUID, LazyLocation> storage_last_scroll_teleport = new HashMap<>();

	public static class UnstableScrollVariant extends CustomItemVariant<Trifles, UnstableScroll, SingleVariant> {

		@ConfigInt(def = 6000, min = 0, desc = "Cooldown in milliseconds until another scroll can be used.")
		private int config_cooldown;

		public UnstableScrollVariant(UnstableScroll parent, SingleVariant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			final var ancient_tome_of_knowledge = CustomItem
				.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(
					AncientTomeOfKnowledge.class,
					BookVariant.BOOK
				)
				.item();

			final var recipe = new ShapedRecipe(recipe_key(), item())
				.shape("pip", "cbe", "plp")
				.setIngredient('b', ancient_tome_of_knowledge)
				.setIngredient('p', Material.MAP)
				.setIngredient('i', Material.CHORUS_FRUIT)
				.setIngredient('c', Material.COMPASS)
				.setIngredient('e', Material.ENDER_PEARL)
				.setIngredient('l', Material.CLOCK);

			add_recipe(recipe);
		}

		@Override
		public Material base() {
			return Material.CARROT_ON_A_STICK;
		}

		public int cooldown() {
			return config_cooldown;
		}
	}

	public UnstableScroll(Context<Trifles> context) {
		super(context, UnstableScrollVariant::new);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		if (event.useItemInHand() == Event.Result.DENY) {
			return;
		}

		// Get item variant
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		final var variant = this.<UnstableScrollVariant>variant_of(item);
		if (variant == null || !variant.enabled()) {
			return;
		}

		// Never actually use the base item if it's custom!
		event.setUseItemInHand(Event.Result.DENY);

		switch (event.getAction()) {
			default:
				return;
			case RIGHT_CLICK_AIR:
				break;
			case RIGHT_CLICK_BLOCK:
				// Require non-cancelled state (so it won't trigger for block-actions like chests)
				// But allow if the clicked block can't be interacted with in the first place
				if (event.useInteractedBlock() != Event.Result.DENY) {
					final var block = event.getClickedBlock();
					if (block.getType().isInteractable()) {
						return;
					}
					event.setUseInteractedBlock(Event.Result.DENY);
				}
				break;
		}

		// Find last
		final var to_location = storage_last_scroll_teleport.get(player.getUniqueId()).location();
		if (to_location == null) {
			return;
		}

		// Check cooldown
		if (player.getCooldown(variant.base()) > 0) {
			return;
		}

		final var current_location = player.getLocation();
		if (get_module().teleport_from_scroll(player, current_location, to_location)) {
			// Set cooldown
			final var cooldown = ms_to_ticks(variant.cooldown());
			player.setCooldown(variant.base(), (int) cooldown);

			// Damage item
			damage_item(player, item, 1);
			swing_arm(player, event.getHand());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_teleport_scroll(final PlayerTeleportScrollEvent event) {
		storage_last_scroll_teleport.put(event.getPlayer().getUniqueId(), new LazyLocation(event.getFrom()));
		mark_persistent_storage_dirty();
	}
}
