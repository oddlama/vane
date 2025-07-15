package org.oddlama.vane.trifles.items;

import java.io.IOException;
import java.util.EnumSet;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(name = "reinforced_elytra", base = Material.ELYTRA, durability = 864, model_data = 0x760002, version = 1)
public class ReinforcedElytra extends CustomItem<Trifles> {

    @ConfigDouble(def = 6.0, min = 0, desc = "Amount of defense points.")
    private double config_defense_points;

    public ReinforcedElytra(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new SmithingRecipeDefinition("generic")
                .base(Material.ELYTRA)
                .addition(Material.NETHERITE_INGOT)
                .copy_nbt(true)
                .result(key().toString())
        );
    }

    @Override
    public ItemStack updateItemStack(ItemStack item_stack) {
        item_stack.editMeta(meta -> {
            final var modifier_defense = new AttributeModifier(
                namespaced_key("armor"),
                config_defense_points,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.CHEST
            );
            meta.removeAttributeModifier(Attribute.ARMOR, modifier_defense);
            meta.addAttributeModifier(Attribute.ARMOR, modifier_defense);
        });
        return item_stack;
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.ITEM_BURN);
    }
}
