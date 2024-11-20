package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class SouldboundRegistry extends CustomEnchantmentRegistry {

    public SouldboundRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("soulbound", ItemTypeTagKeys.ENCHANTABLE_DURABILITY, 1);
        this.register(freezeEvent);
    }
}
