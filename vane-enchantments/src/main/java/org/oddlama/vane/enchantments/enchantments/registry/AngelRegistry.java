package org.oddlama.vane.enchantments.enchantments.registry;

import java.util.List;

import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.keys.ItemTypeKeys;

public class AngelRegistry extends CustomEnchantmentRegistry {

    public AngelRegistry() {
        super("angel", List.of(ItemTypeKeys.ELYTRA), 5);
    }
    
}
