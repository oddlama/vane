package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;

import java.util.EnumSet;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

import net.kyori.adventure.key.Key;

public abstract class Sickle extends CustomItem<Trifles> {
	@ConfigDouble(def = Double.NaN, desc = "Attack damage modifier.")
	public double config_attack_damage;

	@ConfigDouble(def = Double.NaN, desc = "Attack speed modifier.")
	public double config_attack_speed;

	@ConfigInt(def = -1, min = 0, max = BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX, desc = "Harvest radius.")
	public int config_harvest_radius;

	public Sickle(Context<Trifles> context) {
		super(context);
	}

	@Override
	public ItemStack updateItemStack(ItemStack item_stack) {
		item_stack.editMeta(meta -> {
			final var modifier_damage = new AttributeModifier(
				MODIFIER_UUID_GENERIC_ATTACK_DAMAGE,
				"Tool damage",
				config_attack_damage,
				AttributeModifier.Operation.ADD_NUMBER,
				EquipmentSlot.HAND
			);
			final var modifier_speed = new AttributeModifier(
				MODIFIER_UUID_GENERIC_ATTACK_SPEED,
				"Tool speed",
				config_attack_speed,
				AttributeModifier.Operation.ADD_NUMBER,
				EquipmentSlot.HAND
			);
			meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier_damage);
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier_damage);
			meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, modifier_speed);
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, modifier_speed);
		});
		return item_stack;
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.HOE_TILL, InhibitBehavior.USE_OFFHAND);
	}

	@Override
	public Key itemType() {
		return Key.key(Key.MINECRAFT_NAMESPACE, "item/handheld");
	}
}
