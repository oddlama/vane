package org.oddlama.vane.enchantments.enchantments.registry;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.ItemTypeKeys;

public class WingsRegistry extends CustomEnchantmentRegistry {

    public WingsRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("wings", List.of(ItemTypeKeys.ELYTRA), 4);
        this.exclusive_with(List.of(typedKey("wings")));
        this.register(freezeEvent);
    }
    
}
