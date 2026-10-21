package org.oddlama.vane.enchantments

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.registry.event.RegistryEvents
import org.oddlama.vane.enchantments.enchantments.registry.*

/**
 * The [EnchantmentsBootstrapper] class is responsible for bootstrapping the enchantments plugin by
 * registering event handlers for various enchantment-related events during the plugin's lifecycle.
 */
class EnchantmentsBootstrapper : PluginBootstrap {
    /**
     * The [bootstrap] function is called by the PaperMC framework to bootstrap the plugin. It
     * registers an event handler for the [RegistryEvents.ENCHANTMENT] event, which is fired when
     * enchantments are registered.
     *
     * @param context The [BootstrapContext] provided by the PaperMC framework, containing
     *   information and utilities for bootstrapping the plugin.
     */
    override fun bootstrap(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(
            RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
                AngelRegistry(event)
                GrapplingHookRegistry(event)
                HellBentRegistry(event)
                LeafchopperRegistry(event)
                LightningRegistry(event)
                RakeRegistry(event)
                SeedingRegistry(event)
                WingsRegistry(event)
                SouldboundRegistry(event)
                TakeOffRegistry(event)
                UnbreakableRegistry(event)
            }
        )
    }
}
