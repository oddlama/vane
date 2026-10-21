package org.oddlama.vane.regions.menu

import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions

/** Groups all region-related menu components under one namespace component. */
class RegionMenuGroup(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("Menus")) {
    /** Shared context used to create all menu components. */
    private val menuContext = requireNotNull(getContext())

    @JvmField
    /** Name input menu for new regions. */
    val enterRegionNameMenu = EnterRegionNameMenu(menuContext)

    @JvmField
    /** Name input menu for new region groups. */
    val enterRegionGroupNameMenu = EnterRegionGroupNameMenu(menuContext)

    @JvmField
    /** Name input menu for new roles. */
    val enterRoleNameMenu = EnterRoleNameMenu(menuContext)

    @JvmField
    /** Root regions menu. */
    val mainMenu = MainMenu(menuContext)

    @JvmField
    /** Region-group detail menu. */
    val regionGroupMenu = RegionGroupMenu(menuContext)

    @JvmField
    /** Region detail menu. */
    val regionMenu = RegionMenu(menuContext)

    @JvmField
    /** Role detail menu. */
    val roleMenu = RoleMenu(menuContext)

    /** No-op lifecycle hook. */
    override fun onEnable() {}

    /** No-op lifecycle hook. */
    override fun onDisable() {}
}
