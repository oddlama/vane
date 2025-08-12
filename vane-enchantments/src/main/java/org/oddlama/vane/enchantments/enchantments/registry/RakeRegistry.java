package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class RakeRegistry extends CustomEnchantmentRegistry {

    public RakeRegistry(RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> composeEvent) {
        super("rake", ItemTypeTagKeys.HOES, 4);
        this.register(composeEvent);
    }
}
