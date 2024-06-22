package org.oddlama.vane.core;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.enchantments.EnchantmentManager;


import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import net.kyori.adventure.key.Key;

/**
 * CoreBootstrapper
 */
public class CoreBootstrapper implements PluginBootstrap{


    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
            event.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("a:a")), e -> e.activeSlots());
        }));
    }

    // @Override
    // public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        
    // }
}