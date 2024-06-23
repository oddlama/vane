package org.oddlama.vane.enchantments.enchantments.registry;

import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;

public class LightningRegistry extends CustomEnchantmentRegistry {
    public LightningRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("lightning", ItemTypeTagKeys.ENCHANTABLE_SWORD, 1);
        this.register(freezeEvent);
    }
}
