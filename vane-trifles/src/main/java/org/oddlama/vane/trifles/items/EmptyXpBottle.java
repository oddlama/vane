package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.raytrace_dominant_face;
import static org.oddlama.vane.util.Util.exp_for_level;
import static org.oddlama.vane.util.BlockUtil.raytrace_oct;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.give_item;
import static org.oddlama.vane.util.PlayerUtil.remove_one_item_from_hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

@VaneItem(name = "empty_xp_bottle")
public class EmptyXpBottle extends CustomItem<Trifles, EmptyXpBottle> {
	public static enum Variant implements ItemVariantEnum {
		SMALL,
		MEDIUM,
		LARGE;

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return true; }
	}

	public static class EmptyXpBottleVariant extends CustomItemVariant<Trifles, EmptyXpBottle, Variant> {
		public EmptyXpBottleVariant(EmptyXpBottle parent, Variant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			final var item = item();
			final var recipe_key = recipe_key();
			final var recipe = new ShapelessRecipe(recipe_key, item)
				.addIngredient(Material.EXPERIENCE_BOTTLE);

			switch (variant()) {
				case SMALL:  recipe.addIngredient(Material.IRON_NUGGET); break;
				case MEDIUM: recipe.addIngredient(Material.GOLD_NUGGET); break;
				case LARGE:  recipe.addIngredient(Material.BLAZE_POWDER); break;
			}

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
		super(context, Variant.class, Variant.values(), EmptyXpBottleVariant::new);
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

		final var result_variant = XpBottle.Variant.valueOf(variant.variant().name());
		final var xp_bottle_variant = CustomItem.<XpBottle.XpBottleVariant>variant_of(XpBottle.class, result_variant);
		final var exp = (int)((1.0 / (1.0 - config_loss_percentage)) * exp_for_level(xp_bottle_variant.config_capacity));

		// Check if player has enough xp
		if (player.getTotalExperience() < exp) {
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
