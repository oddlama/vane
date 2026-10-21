package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.ItemTypeKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Take Off enchantment, which is applicable to Elytra.
 *
 * @param composeEvent The registry compose event to register the enchantment.
 * @constructor Creates a new TakeOffRegistry instance.
 */
class TakeOffRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("take_off", listOf(ItemTypeKeys.ELYTRA), 3) {
    init {
        register(composeEvent)
    }
}
