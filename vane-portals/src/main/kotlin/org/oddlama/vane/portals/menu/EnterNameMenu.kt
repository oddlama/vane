package org.oddlama.vane.portals.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals

/**
 * Anvil-based menu used to input or rename a portal name.
 *
 * @property langTitle localized title shown on the anvil menu.
 * @property configMaterial preview item used in the input slot.
 */
class EnterNameMenu(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.namespace("EnterName")) {
    /** Localized title shown on the name input menu. */
    @LangMessage var langTitle: TranslatedMessage? = null

    /** Preview item used in the anvil input slot. */
    @ConfigMaterial(def = Material.ENDER_PEARL, desc = "The item used to name portals.")
    var configMaterial: Material? = null

    /** Creates a name input menu using the default placeholder name. */
    fun create(player: Player, onClick: Function2<Player?, String?, Menu.ClickResult?>): Menu =
        create(player, "Name", onClick)

    /** Creates a name input menu and invokes [onClick] with the entered value. */
    fun create(
        player: Player,
        defaultName: String,
        onClick: Function2<Player?, String?, Menu.ClickResult?>,
    ): Menu {
        return MenuFactory.anvilStringInput(
            getContext()!!,
            player,
            langTitle!!.str(),
            ItemStack(configMaterial!!),
            defaultName,
        ) { p: Player?, menu: Menu?, name: String? ->
            menu!!.close(p!!)
            onClick.apply(p, name)
        }
    }

    /** Enables this menu component. */
    public override fun onEnable() {}

    /** Disables this menu component. */
    public override fun onDisable() {}
}
