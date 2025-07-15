package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class GrapplingHookRegistry extends CustomEnchantmentRegistry {

    public GrapplingHookRegistry(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        super("grappling_hook", ItemTypeTagKeys.ENCHANTABLE_FISHING, 3);
        this.register(freezeEvent);
    }
}
