package org.oddlama.vane.enchantments.enchantments.registry;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;

public class UnbreakableRegistry extends CustomEnchantmentRegistry {
    
    public UnbreakableRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("unbreakable", ItemTypeTagKeys.ENCHANTABLE_DURABILITY, 1);
        this.exclusive_with(List.of(EnchantmentKeys.UNBREAKING, EnchantmentKeys.MENDING));
        this.register(freezeEvent);
    }
}
