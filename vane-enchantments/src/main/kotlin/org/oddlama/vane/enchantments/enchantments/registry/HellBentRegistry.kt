package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Hell Bent enchantment, which is applicable to head armor.
 *
 * @param composeEvent The registry compose event to register the enchantment.
 * @constructor Creates a new HellBentRegistry instance.
 */
class HellBentRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("hell_bent", ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR, 1) {
    init {
        register(composeEvent)
    }
}
