package org.oddlama.vane.portals.menu

import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals

/**
 * Holds all menu components used by the portals module.
 *
 * @property enterNameMenu menu used to enter a portal name.
 * @property consoleMenu menu shown when opening a portal console.
 * @property settingsMenu menu used to edit portal settings.
 * @property styleMenu menu used to configure portal styles.
 */
class PortalMenuGroup(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.namespace("Menus")) {
    /** Context scoped to this menu group. */
    private val ctx = getContext()!!

    /** Menu used to enter a portal name. */
    @JvmField var enterNameMenu: EnterNameMenu? = EnterNameMenu(ctx)

    /** Menu shown when opening a portal console. */
    @JvmField var consoleMenu: ConsoleMenu? = ConsoleMenu(ctx)

    /** Menu used to edit portal settings. */
    @JvmField var settingsMenu: SettingsMenu? = SettingsMenu(ctx)

    /** Menu used to configure portal styles. */
    @JvmField var styleMenu: StyleMenu? = StyleMenu(ctx)

    /** Enables this menu group component. */
    public override fun onEnable() {}

    /** Disables this menu group component. */
    public override fun onDisable() {}
}
