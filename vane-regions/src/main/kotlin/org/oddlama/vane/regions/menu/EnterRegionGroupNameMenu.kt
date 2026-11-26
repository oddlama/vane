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

/**
 * Anvil-based prompt menu for entering a region-group name.
 */
class EnterRegionGroupNameMenu(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("EnterRegionGroupName")) {
    /**
     * Localized title shown in the anvil input UI.
     */
    @LangMessage
    var langTitle: TranslatedMessage? = null

    /**
     * Icon material used in the anvil input UI.
     */
    @ConfigMaterial(def = Material.GLOBE_BANNER_PATTERN, desc = "The item used to name region groups.")
    var configMaterial: Material? = null

    /**
     * Non-null context for creating child menus.
     */
    private val menuContext get() = requireNotNull(getContext())

    /**
     * Resolved anvil UI title.
     */
    private val title get() = requireNotNull(langTitle).str()

    /**
     * Resolved anvil icon item.
     */
    private val icon get() = ItemStack(requireNotNull(configMaterial))

    /**
     * Creates a name-input menu with default region-group name seed.
     */
    fun create(player: Player, onClick: Function2<Player?, String?, Menu.ClickResult?>): Menu {
        return create(player, "Group", onClick)
    }

    /**
     * Creates a name-input menu using the provided default value.
     */
    fun create(
        player: Player,
        defaultName: String,
        onClick: Function2<Player?, String?, Menu.ClickResult?>
    ): Menu = MenuFactory.anvilStringInput(menuContext, player, title, icon, defaultName) { p, menu, name ->
        /** Player passed by menu callback. */
        val playerRef = p ?: return@anvilStringInput null
        menu?.close(playerRef)
        onClick.apply(playerRef, name)
    }

    /**
     * No-op lifecycle hook.
     */
    override fun onEnable() {}

    /**
     * No-op lifecycle hook.
     */
    override fun onDisable() {}
}
