package org.oddlama.vane.core

import org.bukkit.event.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Base module component that auto-registers and unregisters as a Bukkit listener.
 *
 * @param T owning module type.
 * @param context listener context.
 */
open class Listener<T : Module<T?>?>(context: Context<T?>?) :
    ModuleComponent<T?>(context), Listener {
    /** Registers this listener with the module. */
    override fun onEnable() {
        module?.registerListener(this)
    }

    /** Unregisters this listener from the module. */
    override fun onDisable() {
        module?.unregisterListener(this)
    }
}
