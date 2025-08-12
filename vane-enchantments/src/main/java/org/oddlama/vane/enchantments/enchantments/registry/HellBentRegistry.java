package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class HellBentRegistry extends CustomEnchantmentRegistry {

    public HellBentRegistry(RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> composeEvent) {
        super("hell_bent", ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR, 1);
        this.register(composeEvent);
    }
}
