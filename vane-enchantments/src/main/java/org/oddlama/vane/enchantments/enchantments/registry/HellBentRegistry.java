package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class HellBentRegistry extends CustomEnchantmentRegistry {

    public HellBentRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("hell_bent", ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR, 1);
        this.register(freezeEvent);
    }
}
