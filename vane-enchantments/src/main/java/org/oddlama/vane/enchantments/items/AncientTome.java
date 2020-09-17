package org.oddlama.vane.enchantments.items;

import static org.oddlama.vane.util.BlockUtil.relative;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerTeleportEvent;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ShapelessRecipe;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.BlockUtil;

@VaneItem(name = "ancient_tome")
public class AncientTome extends CustomItem<Enchantments, AncientTome> {
	public static class AncientTomeVariant extends CustomItemVariant<Enchantments, AncientTome, BookVariant> {
		public AncientTomeVariant(AncientTome parent, BookVariant variant) {
			super(parent, variant);
		}

		@Override
		public Material base() {
			switch (variant()) {
				default:             throw new RuntimeException("Missing variant case. This is a bug.");
				case BOOK:           return Material.BOOK;
				case ENCHANTED_BOOK: return Material.ENCHANTED_BOOK;
			}
		}
	}

	public AncientTome(Context<Enchantments> context) {
		super(context, BookVariant.class, BookVariant.values(), AncientTomeVariant::new);
	}
}