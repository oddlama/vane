package org.oddlama.vane.enchantments.enchantments.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.keys.ItemTypeKeys
import net.kyori.adventure.key.Key
import org.bukkit.enchantments.Enchantment
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry

/**
 * Registry for the Angel enchantment, which is exclusive with the Wings enchantment.
 *
 * @param composeEvent The registry compose event to register the enchantment.
 * @constructor Creates an AngelRegistry instance.
 */
class AngelRegistry(
    composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
) : CustomEnchantmentRegistry("angel", listOf(ItemTypeKeys.ELYTRA), 5) {
    init {
        exclusiveWith(
                listOf(
                    TypedKey.create(
                        RegistryKey.ENCHANTMENT,
                        Key.key(
                            NAMESPACE,
                            "wings",
                        ),
                    )
                )
            )
            .register(composeEvent)
    }
}
