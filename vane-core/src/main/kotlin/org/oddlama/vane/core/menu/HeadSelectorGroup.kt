package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Configuration group for head selector menu resources.
 *
 * @param context component context.
 */
class HeadSelectorGroup(context: Context<Core?>) :
    ModuleComponent<Core?>(
        context.namespace("HeadSelector", "Menu configuration for the head selector menu.")
    ) {

    /** Localized head selector menu title. */
    @LangMessage lateinit var langTitle: TranslatedMessage

    /** Localized filter input title used by the head selector. */
    @LangMessage lateinit var langFilterTitle: TranslatedMessage

    /** Translated item used to represent a selectable head entry. */
    @JvmField
    var itemSelectHead: TranslatedItemStack<*> =
        TranslatedItemStack<Core?>(
            getContext()!!,
            "SelectHead",
            Material.BARRIER,
            1,
            "Used to represent a head in the head library.",
        )

    /** Enables this component. */
    override fun onEnable() {}

    /** Disables this component. */
    override fun onDisable() {}
}
