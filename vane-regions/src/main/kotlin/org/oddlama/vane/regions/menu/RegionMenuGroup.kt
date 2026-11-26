package org.oddlama.vane.regions.menu

import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions

/**
 * Groups all region-related menu components under one namespace component.
 */
class RegionMenuGroup(context: Context<Regions?>) : ModuleComponent<Regions?>(context.namespace("Menus")) {
    /**
     * Shared context used to create all menu components.
     */
    private val menuContext = requireNotNull(getContext())

    /**
     * Name input menu for new regions.
     */
    @JvmField
    val enterRegionNameMenu = EnterRegionNameMenu(menuContext)

    /**
     * Name input menu for new region groups.
     */
    @JvmField
    val enterRegionGroupNameMenu = EnterRegionGroupNameMenu(menuContext)

    /**
     * Name input menu for new roles.
     */
    @JvmField
    val enterRoleNameMenu = EnterRoleNameMenu(menuContext)

    /**
     * Root regions menu.
     */
    @JvmField
    val mainMenu = MainMenu(menuContext)

    /**
     * Region-group detail menu.
     */
    @JvmField
    val regionGroupMenu = RegionGroupMenu(menuContext)

    /**
     * Region detail menu.
     */
    @JvmField
    val regionMenu = RegionMenu(menuContext)

    /**
     * Role detail menu.
     */
    @JvmField
    val roleMenu = RoleMenu(menuContext)

    /**
     * No-op lifecycle hook.
     */
    override fun onEnable() {}

    /**
     * No-op lifecycle hook.
     */
    override fun onDisable() {}
}
