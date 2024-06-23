package org.oddlama.vane.enchantments;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.enchantments.enchantments.registry.AngelRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.GrapplingHookRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.HellBentRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.LeafchopperRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.LightningRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.RakeRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.SeedingRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.SouldboundRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.TakeOffRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.UnbreakableRegistry;
import org.oddlama.vane.enchantments.enchantments.registry.WingsRegistry;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.event.RegistryEvents;

public class EnchantmentsBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
            new AngelRegistry(event);
            new GrapplingHookRegistry(event);
            new HellBentRegistry(event);
            new LeafchopperRegistry(event);
            new LightningRegistry(event);
            new RakeRegistry(event);
            new SeedingRegistry(event);
            new WingsRegistry(event);
            new SouldboundRegistry(event);
            new TakeOffRegistry(event);
            new UnbreakableRegistry(event);
        }));
    }
    
}
