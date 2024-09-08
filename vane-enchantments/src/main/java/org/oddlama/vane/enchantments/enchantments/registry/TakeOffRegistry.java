package org.oddlama.vane.enchantments.enchantments.registry;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.ItemTypeKeys;

public class TakeOffRegistry extends CustomEnchantmentRegistry {
    public TakeOffRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("take_off", List.of(ItemTypeKeys.ELYTRA), 3);
        this.register(freezeEvent);
    }
}
