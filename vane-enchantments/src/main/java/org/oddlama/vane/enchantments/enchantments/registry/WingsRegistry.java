package org.oddlama.vane.enchantments.enchantments.registry;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import java.util.List;
import org.bukkit.enchantments.Enchantment;
import org.oddlama.vane.enchantments.CustomEnchantmentRegistry;

public class WingsRegistry extends CustomEnchantmentRegistry {

    public WingsRegistry(RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> composeEvent) {
        super("wings", List.of(ItemTypeKeys.ELYTRA), 4);
        this.exclusive_with(List.of(typedKey("wings")));
        this.register(composeEvent);
    }
}
