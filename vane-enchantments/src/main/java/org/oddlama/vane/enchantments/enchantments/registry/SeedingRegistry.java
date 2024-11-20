package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class SeedingRegistry extends CustomEnchantmentRegistry {

    public SeedingRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("seeding", ItemTypeTagKeys.HOES, 4);
        this.register(freezeEvent);
    }
}
