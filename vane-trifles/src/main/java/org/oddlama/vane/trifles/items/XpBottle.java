package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.raytrace_dominant_face;
import static org.oddlama.vane.util.BlockUtil.raytrace_oct;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.Util.exp_for_level;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

@VaneItem(name = "xp_bottle")
public class XpBottle extends CustomItem<Trifles, XpBottle> {
	public static enum Variant implements ItemVariantEnum {
		SMALL,
		MEDIUM,
		LARGE;

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return true; }
	}

	public static class XpBottleVariant extends CustomItemVariant<Trifles, XpBottle, Variant> {
		@ConfigInt(def = -1, desc = "Level capacity.")
		public int config_capacity;

		public XpBottleVariant(XpBottle parent, Variant variant) {
			super(parent, variant);
		}

		@Override
		public Material base() {
			return Material.HONEY_BOTTLE;
		}

		@Override
		public BaseComponent display_name() {
			final var name = super.display_name();
			name.setColor(ChatColor.YELLOW);
			return name;
		}

		public int config_capacity_def() {
			switch (variant()) {
				default:     throw new RuntimeException("Missing variant case. This is a bug.");
				case SMALL:  return 10;
				case MEDIUM: return 20;
				case LARGE:  return 30;
			}
		}
	}

	public XpBottle(Context<Trifles> context) {
		super(context, Variant.class, Variant.values(), XpBottleVariant::new);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_player_item_consume(final PlayerItemConsumeEvent event) {
		final var player = event.getPlayer();

		// Get item variant
		final var item = event.getItem();
		final var variant = this.<XpBottleVariant>variant_of(item);
		if (variant == null || !variant.enabled()) {
			return;
		}

		// Add player experience without applying mending effects
		player.giveExp(exp_for_level(variant.config_capacity), false);
		// TODO remove from hand
		// TODO give back bottle
		event.setCancelled(true);
	}
}
