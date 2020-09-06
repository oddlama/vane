package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.give_item;
import static org.oddlama.vane.util.PlayerUtil.remove_one_item_from_hand;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.Util.exp_for_level;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ShapelessRecipe;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "empty_xp_bottle")
public class EmptyXpBottle extends CustomItem<Trifles, EmptyXpBottle> {
	public static class EmptyXpBottleVariant extends CustomItemVariant<Trifles, EmptyXpBottle, SingleVariant> {
		public EmptyXpBottleVariant(EmptyXpBottle parent, SingleVariant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			final var item = item();
			final var recipe_key = recipe_key();
			final var recipe = new ShapelessRecipe(recipe_key, item)
				.addIngredient(Material.EXPERIENCE_BOTTLE)
				.addIngredient(Material.GLASS_BOTTLE)
				.addIngredient(Material.GOLD_NUGGET);

			add_recipe(recipe_key, recipe);
		}

		@Override
		public Material base() {
			return Material.GLASS_BOTTLE;
		}
	}

	@ConfigDouble(def = 0.3, min = 0.0, max = 0.999, desc = "Percentage of lost experience while bottling. For 10% loss, bottling 30 levels will require 30 * (1 / (1 - 0.1)) = 33.33 levels")
	public double config_loss_percentage;

	public EmptyXpBottle(Context<Trifles> context) {
		super(context, EmptyXpBottleVariant::new);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
	public void on_player_right_click(final PlayerInteractEvent event) {
		// Get item variant
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		final var variant = this.<EmptyXpBottleVariant>variant_of(item);
		if (variant == null || !variant.enabled()) {
			return;
		}

		// Nevera actually use the base item if it's custom!
		event.setUseItemInHand(Event.Result.DENY);

		switch (event.getAction()) {
			default: return;
			case RIGHT_CLICK_AIR: break;
			case RIGHT_CLICK_BLOCK:
				// Require non-cancelled state (so it won't trigger for block-actions like chests)
				// Second check prevent original item usage (collecting liquids)
				if (event.useInteractedBlock() != Event.Result.DENY) {
					return;
				}
				break;
		}

		// Check if last consume time is too recent, to prevent accidential re-filling
		final var now = System.currentTimeMillis();
		final var last_consume = get_module().last_xp_bottle_consume_time.getOrDefault(player.getUniqueId(), 0l);
		if (now - last_consume < 1000) {
			return;
		}

		// Find maximum fitting capacity
		XpBottle.XpBottleVariant xp_bottle_variant = null;
		int exp = 0;
		for (final var xpvar : XpBottle.Variant.values()) {
			var cur_xp_bottle_variant = CustomItem.<XpBottle.XpBottleVariant>variant_of(XpBottle.class, xpvar);
			var cur_exp = (int)((1.0 / (1.0 - config_loss_percentage)) * exp_for_level(cur_xp_bottle_variant.config_capacity));

			// Check if player has enough xp and this variant has more than the last
			if (player.getTotalExperience() >= cur_exp && cur_exp > exp) {
				exp = cur_exp;
				xp_bottle_variant = cur_xp_bottle_variant;
			}
		}

		// Check if there was a fitting bottle
		if (xp_bottle_variant == null) {
			return;
		}

		// Take xp, take item, play sound, give item.
		player.giveExp(-exp, false);
		remove_one_item_from_hand(player, event.getHand());
		give_item(player, xp_bottle_variant.item());
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 4.0f);
		swing_arm(player, event.getHand());
	}
}
