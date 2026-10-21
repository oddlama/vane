package org.oddlama.vane.regions.menu

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
import org.oddlama.vane.regions.Regions

/** Anvil-based prompt menu for entering a role name. */
class EnterRoleNameMenu(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("EnterRoleName")) {
    @LangMessage
    /** Localized title shown in the anvil input UI. */
    var langTitle: TranslatedMessage? = null

    @ConfigMaterial(def = Material.BOOK, desc = "The item used to name roles.")
    /** Icon material used in the anvil input UI. */
    var configMaterial: Material? = null

    /** Non-null context for creating child menus. */
    private val menuContext
        get() = requireNotNull(getContext())

    /** Resolved anvil UI title. */
    private val title
        get() = requireNotNull(langTitle).str()

    /** Resolved anvil icon item. */
    private val icon
        get() = ItemStack(requireNotNull(configMaterial))

    /** Creates a name-input menu with default role name seed. */
    fun create(player: Player, onClick: Function2<Player?, String?, Menu.ClickResult?>): Menu {
        return create(player, "Role", onClick)
    }

    /** Creates a name-input menu using the provided default value. */
    fun create(
        player: Player,
        defaultName: String,
        onClick: Function2<Player?, String?, Menu.ClickResult?>,
    ): Menu =
        MenuFactory.anvilStringInput(menuContext, player, title, icon, defaultName) { p, menu, name
            ->
            /** Player passed by menu callback. */
            val playerRef = p ?: return@anvilStringInput null
            menu?.close(playerRef)
            onClick.apply(playerRef, name)
        }

    /** No-op lifecycle hook. */
    override fun onEnable() {}

    /** No-op lifecycle hook. */
    override fun onDisable() {}
}
