package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Soulbound enchantment.
 *
 * @param composeEvent The event used to compose the registry.
 * @constructor Creates a new instance of [SouldboundRegistry] and registers the enchantment using
 *   the provided [composeEvent].
 */
class SouldboundRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("soulbound", ItemTypeTagKeys.ENCHANTABLE_DURABILITY, 1) {
    init {
        register(composeEvent)
    }
}
