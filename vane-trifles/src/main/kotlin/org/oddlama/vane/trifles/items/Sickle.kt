package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.BlockUtil

/** Base sickle item that applies configurable combat stats and harvest radius. */
abstract class Sickle(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    /** Attack damage modifier applied while holding this sickle. */
    @ConfigDouble(def = Double.NaN, desc = "Attack damage modifier.")
    var configAttackDamage: Double = 0.0

    /** Attack speed modifier applied while holding this sickle. */
    @ConfigDouble(def = Double.NaN, desc = "Attack speed modifier.")
    var configAttackSpeed: Double = 0.0

    /** Radius used when harvesting nearby seeded plants. */
    @JvmField
    @ConfigInt(
        def = -1,
        min = 0,
        max = BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX,
        desc = "Harvest radius.",
    )
    var configHarvestRadius: Int = 0

    /** Applies attack attribute modifiers to the sickle item stack. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        itemStack.editMeta { meta: ItemMeta ->
            val modifierDamage =
                AttributeModifier(
                    namespacedKey("attack_damage"),
                    configAttackDamage,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND,
                )
            val modifierSpeed =
                AttributeModifier(
                    namespacedKey("attack_speed"),
                    configAttackSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND,
                )
            meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE, modifierDamage)
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifierDamage)
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED, modifierSpeed)
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, modifierSpeed)
        }
        return itemStack
    }

    /** Prevents conflicting vanilla interactions for sickles. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.HOE_TILL,
            InhibitBehavior.USE_OFFHAND,
        )
    }
}
