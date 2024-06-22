package org.oddlama.vane.enchantments;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.enchantments.enchantments.registry.AngelRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.RakeRegistry;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.event.RegistryEvents;

public class EnchantmentsBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
            new RakeRegistry().register(event);
            new AngelRegistry().register(event);
        }));
    }
    
}
