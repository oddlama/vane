package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Lightning enchantment, extending the CustomEnchantmentRegistry.
 *
 * @param composeEvent The registry compose event to register the enchantment.
 * @constructor Creates a new LightningRegistry instance.
 */
class LightningRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("lightning", ItemTypeTagKeys.SWORDS, 1) {
    init {
        register(composeEvent)
    }
}
