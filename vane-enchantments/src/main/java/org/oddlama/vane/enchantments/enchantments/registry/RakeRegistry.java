package org.oddlama.vane.enchantments.enchantments.registry;

import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;

public class RakeRegistry extends CustomEnchantmentRegistry {

    public RakeRegistry() {
        super("rake",
                ItemTypeTagKeys.HOES,
                4);
    }

}