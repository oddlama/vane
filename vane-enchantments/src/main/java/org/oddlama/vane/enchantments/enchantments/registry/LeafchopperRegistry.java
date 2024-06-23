package org.oddlama.vane.enchantments.enchantments.registry;

import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;

public class LeafchopperRegistry extends CustomEnchantmentRegistry{
    public LeafchopperRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("leafchopper", ItemTypeTagKeys.AXES, 1);
        this.register(freezeEvent);
    }
}
