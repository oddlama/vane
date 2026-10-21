package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(
    name = "reinforced_elytra",
    base = Material.ELYTRA,
    durability = 864,
    modelData = 0x760002,
    version = 1,
)
/** Elytra upgrade that adds configurable armor points. */
class ReinforcedElytra(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    /** Extra armor points applied while the item is worn in chest slot. */
    @ConfigDouble(def = 6.0, min = 0.0, desc = "Amount of defense points.")
    var configDefensePoints: Double = 0.0

    /** Defines the smithing recipe that upgrades an elytra to reinforced variant. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            SmithingRecipeDefinition("generic")
                .base(Material.ELYTRA)
                .addition(Material.NETHERITE_INGOT)
                .copyNbt(true)
                .result(key().toString())
        )
    }

    /** Applies armor attribute modifiers to the item stack metadata. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        itemStack.editMeta { meta: ItemMeta ->
            val modifierDefense =
                AttributeModifier(
                    namespacedKey("armor"),
                    configDefensePoints,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.CHEST,
                )
            meta.removeAttributeModifier(Attribute.ARMOR, modifierDefense)
            meta.addAttributeModifier(Attribute.ARMOR, modifierDefense)
        }
        return itemStack
    }

    /** Prevents unintended vanilla behavior interactions for reinforced elytras. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.ITEM_BURN)
    }
}
