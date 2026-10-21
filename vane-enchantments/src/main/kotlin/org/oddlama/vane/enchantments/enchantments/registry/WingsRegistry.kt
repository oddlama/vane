package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.ItemTypeKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Wings enchantment, extending the CustomEnchantmentRegistry.
 *
 * @param composeEvent The compose event for the registry entry.
 * @constructor Creates a new WingsRegistry.
 */
class WingsRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("wings", listOf(ItemTypeKeys.ELYTRA), 4) {
    init {
        exclusiveWith(listOf(typedKey("wings")))
        register(composeEvent)
    }
}
