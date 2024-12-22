package org.oddlama.vane.trifles.items;

import java.util.EnumSet;
import net.kyori.adventure.key.Key;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

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
                namespaced_key("attack_damage"),
                config_attack_damage,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HAND
            );
            final var modifier_speed = new AttributeModifier(
                namespaced_key("attack_speed"),
                config_attack_speed,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HAND
            );
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_damage);
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier_damage);
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED, modifier_speed);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier_speed);
        });
        return item_stack;
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.HOE_TILL, InhibitBehavior.USE_OFFHAND);
    }
}
