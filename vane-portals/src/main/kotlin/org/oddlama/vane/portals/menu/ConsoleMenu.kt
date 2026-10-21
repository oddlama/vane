package org.oddlama.vane.portals.menu

import java.util.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent
import org.oddlama.vane.portals.event.PortalDestroyEvent
import org.oddlama.vane.portals.event.PortalSelectTargetEvent
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.portals.portal.Portal.TargetSelectionComparator
import org.oddlama.vane.util.StorageUtil

/** Main portal console menu with actions for target selection, settings, unlinking, and destroy. */
class ConsoleMenu(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.namespace("Console")) {
    /** Title template for the main console menu. */
    @LangMessage var langTitle: TranslatedMessage? = null

    /** Title used for unlink confirmation dialogs. */
    @LangMessage var langUnlinkConsoleConfirmTitle: TranslatedMessage? = null

    /** Title used for portal destruction confirmation dialogs. */
    @LangMessage var langDestroyPortalConfirmTitle: TranslatedMessage? = null

    /** Title used by target selection selector menus. */
    @LangMessage var langSelectTargetTitle: TranslatedMessage? = null

    /** Prompt used by target-selector filter input. */
    @LangMessage var langFilterPortalsTitle: TranslatedMessage? = null

    /** Visibility label for public portals in selectors. */
    @LangMessage var langSelectTargetPortalVisibilityPublic: TranslatedMessage? = null

    /** Visibility label for private portals in selectors. */
    @LangMessage var langSelectTargetPortalVisibilityPrivate: TranslatedMessage? = null

    /** Visibility label for group portals in selectors. */
    @LangMessage var langSelectTargetPortalVisibilityGroup: TranslatedMessage? = null

    /** Visibility label for group-internal portals in selectors. */
    @LangMessage var langSelectTargetPortalVisibilityGroupInternal: TranslatedMessage? = null

    /** Menu item opening portal settings. */
    var itemSettings: TranslatedItemStack<*>

    /** Menu item opening target selection. */
    var itemSelectTarget: TranslatedItemStack<*>

    /** Menu item representation for a selectable target portal. */
    var itemSelectTargetPortal: TranslatedItemStack<*>

    /** Disabled/locked variant of target selection item. */
    var itemSelectTargetLocked: TranslatedItemStack<*>

    /** Menu item opening unlink-console confirmation. */
    var itemUnlinkConsole: TranslatedItemStack<*>

    /** Accept item for unlink-console confirmation. */
    var itemUnlinkConsoleConfirmAccept: TranslatedItemStack<*>

    /** Cancel item for unlink-console confirmation. */
    var itemUnlinkConsoleConfirmCancel: TranslatedItemStack<*>

    /** Menu item opening destroy-portal confirmation. */
    var itemDestroyPortal: TranslatedItemStack<*>

    /** Accept item for destroy-portal confirmation. */
    var itemDestroyPortalConfirmAccept: TranslatedItemStack<*>

    /** Cancel item for destroy-portal confirmation. */
    var itemDestroyPortalConfirmCancel: TranslatedItemStack<*>

    /** Initializes all translated menu item definitions used by this component. */
    init {
        val ctx = getContext()!!
        itemSettings =
            TranslatedItemStack<Portals?>(
                ctx,
                "Settings",
                Material.WRITABLE_BOOK,
                1,
                "Used to enter portal settings.",
            )
        itemSelectTarget =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectTarget",
                Material.COMPASS,
                1,
                "Used to enter portal target selection.",
            )
        itemSelectTargetPortal =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectTargetPortal",
                Material.COMPASS,
                1,
                "Used to represent a portal in the target selection menu.",
            )
        itemSelectTargetLocked =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectTargetLocked",
                Material.FIREWORK_STAR,
                1,
                "Used to show portal target selection when the target is locked.",
            )
        itemUnlinkConsole =
            TranslatedItemStack<Portals?>(
                ctx,
                "UnlinkConsole",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to unlink the current console.",
            )
        itemUnlinkConsoleConfirmAccept =
            TranslatedItemStack<Portals?>(
                ctx,
                "UnlinkConsoleConfirmAccept",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to confirm unlinking the current console.",
            )
        itemUnlinkConsoleConfirmCancel =
            TranslatedItemStack<Portals?>(
                ctx,
                "UnlinkConsoleConfirmCancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel unlinking the current console.",
            )
        itemDestroyPortal =
            TranslatedItemStack<Portals?>(
                ctx,
                "DestroyPortal",
                Material.TNT,
                1,
                "Used to destroy the portal.",
            )
        itemDestroyPortalConfirmAccept =
            TranslatedItemStack<Portals?>(
                ctx,
                "DestroyPortalConfirmAccept",
                Material.TNT,
                1,
                "Used to confirm destroying the portal.",
            )
        itemDestroyPortalConfirmCancel =
            TranslatedItemStack<Portals?>(
                ctx,
                "DestroyPortalConfirmCancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel destroying the portal.",
            )
    }

    /** Creates the main console menu for [portal] opened by [player] at [console]. */
    fun create(portal: Portal, player: Player, console: Block?): Menu {
        val columns = 9
        val title = langTitle!!.strComponent("§5§l" + portal.name())
        val consoleMenu = Menu(getContext()!!, Bukkit.createInventory(null, columns, title))
        consoleMenu.tag(PortalMenuTag(portal.id()))

        // Check if target selection would be allowed
        val selectTargetEvent = PortalSelectTargetEvent(player, portal, null, true)
        module!!.server.pluginManager.callEvent(selectTargetEvent)
        if (!selectTargetEvent.isCancelled || player.hasPermission(module!!.adminPermission)) {
            consoleMenu.add(menuItemSelectTarget(portal))
        }

        // Check if settings would be allowed
        val settingsEvent = PortalChangeSettingsEvent(player, portal, true)
        module!!.server.pluginManager.callEvent(settingsEvent)
        if (!settingsEvent.isCancelled() || player.hasPermission(module!!.adminPermission)) {
            consoleMenu.add(menuItemSettings(portal, console))
        }

        // Check if unlink would be allowed
        val unlinkEvent = PortalUnlinkConsoleEvent(player, console, portal, true)
        module!!.server.pluginManager.callEvent(unlinkEvent)
        if (!unlinkEvent.isCancelled() || player.hasPermission(module!!.adminPermission)) {
            consoleMenu.add(menuItemUnlinkConsole(portal, console))
        }

        // Check if destroy would be allowed
        val destroyEvent = PortalDestroyEvent(player, portal, true)
        module!!.server.pluginManager.callEvent(destroyEvent)
        if (!destroyEvent.isCancelled() || player.hasPermission(module!!.adminPermission)) {
            consoleMenu.add(menuItemDestroyPortal(portal))
        }

        return consoleMenu
    }

    /** Builds the settings action menu item. */
    private fun menuItemSettings(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(
            0,
            itemSettings.item(),
            Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                val settingsEvent = PortalChangeSettingsEvent(player!!, portal, false)
                module!!.server.pluginManager.callEvent(settingsEvent)
                if (
                    settingsEvent.isCancelled() && !player.hasPermission(module!!.adminPermission)
                ) {
                    module!!.langSettingsRestricted?.send(player)
                    return@Function3 Menu.ClickResult.ERROR
                }

                menu!!.close(player)
                module!!.menus!!.settingsMenu!!.create(portal, player, console).open(player)
                Menu.ClickResult.SUCCESS
            },
        )
    }

    /** Returns localized visibility text component for [visibility]. */
    private fun portalVisibility(visibility: Portal.Visibility): Component {
        return (when (visibility) {
                Portal.Visibility.PUBLIC -> langSelectTargetPortalVisibilityPublic
                Portal.Visibility.GROUP -> langSelectTargetPortalVisibilityGroup
                Portal.Visibility.GROUP_INTERNAL -> langSelectTargetPortalVisibilityGroupInternal
                Portal.Visibility.PRIVATE -> langSelectTargetPortalVisibilityPrivate
            })!!
            .format()
    }

    /** Builds the target-selection action item and selector flow. */
    private fun menuItemSelectTarget(portal: Portal): MenuWidget {
        return object :
            MenuItem(
                4,
                null,
                Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                    if (portal.targetLocked()) {
                        return@Function3 Menu.ClickResult.ERROR
                    } else {
                        menu!!.close(player!!)
                        val allPortals: MutableList<Portal?> =
                            module!!
                                .allAvailablePortals()
                                .asSequence()
                                .filterNotNull()
                                .filter { p ->
                                    val vis = p.visibility()
                                    when (vis) {
                                        Portal.Visibility.PUBLIC -> true
                                        Portal.Visibility.GROUP ->
                                            module!!.playerCanUsePortalsInRegionGroupOf(player, p)
                                        Portal.Visibility.GROUP_INTERNAL ->
                                            module!!.isInSameRegionGroup(portal, p)
                                        Portal.Visibility.PRIVATE -> player.uniqueId == p.owner()
                                        null -> false
                                    }
                                }
                                .filter { p -> p.id() != portal.id() }
                                .sortedWith(TargetSelectionComparator(player))
                                .map { p -> p as Portal? }
                                .toMutableList()

                        val filter =
                            Filter.StringFilter({ p: Portal?, str: String? ->
                                val pname = p?.name()
                                !(pname == null || str == null) &&
                                    pname.lowercase(Locale.getDefault()).contains(str)
                            })
                        MenuFactory.genericSelector<Portal?, Filter.StringFilter<Portal?>?>(
                                getContext()!!,
                                player,
                                langSelectTargetTitle!!.str(),
                                langFilterPortalsTitle!!.str(),
                                allPortals,
                                { p: Portal? ->
                                    val dist =
                                        p!!
                                            .spawn()
                                            .toVector()
                                            .setY(0.0)
                                            .distance(player.location.toVector().setY(0.0))
                                    itemSelectTargetPortal.alternative(
                                        module!!.iconFor(p) ?: ItemStack(Material.BARRIER),
                                        "§a§l" + p.name(),
                                        "§6" + String.format("%.1f", dist),
                                        "§b" + p.spawn().world.name,
                                        portalVisibility(
                                            p.visibility() ?: Portal.Visibility.PRIVATE
                                        ),
                                    )
                                },
                                filter,
                                { player2: Player?, m: Menu?, t: Portal? ->
                                    m!!.close(player2!!)
                                    val selectTargetEvent =
                                        PortalSelectTargetEvent(player2, portal, t, false)
                                    module!!.server.pluginManager.callEvent(selectTargetEvent)
                                    if (
                                        selectTargetEvent.isCancelled &&
                                            !player2.hasPermission(module!!.adminPermission)
                                    ) {
                                        module!!.langSelectTargetRestricted?.send(player2)
                                        return@genericSelector Menu.ClickResult.ERROR
                                    }

                                    portal.targetId(t!!.id())

                                    // Update portal block to reflect new target on consoles
                                    portal.updateBlocks(module!!)
                                    Menu.ClickResult.SUCCESS
                                },
                                { player2: Player? -> menu.open(player2!!) },
                            )
                            .tag(PortalMenuTag(portal.id()))
                            .open(player)
                        return@Function3 Menu.ClickResult.SUCCESS
                    }
                },
            ) {
            override fun item(item: ItemStack?) {
                val target = portal.target(module!!)
                val targetName = "§a" + (if (target == null) "None" else target.name())
                if (portal.targetLocked()) {
                    super.item(itemSelectTargetLocked.item(targetName))
                } else {
                    super.item(itemSelectTarget.item(targetName))
                }
            }
        }
    }

    /** Builds the unlink-console action item and confirmation flow. */
    private fun menuItemUnlinkConsole(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(7, itemUnlinkConsole.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            MenuFactory.confirm(
                    getContext()!!,
                    langUnlinkConsoleConfirmTitle!!.str(),
                    itemUnlinkConsoleConfirmAccept.item(),
                    { player2: Player? ->
                        // Call event
                        val event = PortalUnlinkConsoleEvent(player2!!, console, portal, false)
                        module!!.server.pluginManager.callEvent(event)
                        if (
                            event.isCancelled() && !player2.hasPermission(module!!.adminPermission)
                        ) {
                            module!!.langUnlinkRestricted?.send(player2)
                            return@confirm Menu.ClickResult.ERROR
                        }

                        val portalBlock =
                            portal.portalBlockFor(console)
                                ?: // The Console was likely already removed by another
                                // player
                                return@confirm Menu.ClickResult.ERROR

                        module!!.removePortalBlock(portal, portalBlock)
                        Menu.ClickResult.SUCCESS
                    },
                    itemUnlinkConsoleConfirmCancel.item(),
                    { player2: Player? -> menu.open(player2!!) },
                )
                .tag(PortalMenuTag(portal.id()))
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the destroy-portal action item and confirmation flow. */
    private fun menuItemDestroyPortal(portal: Portal): MenuWidget {
        return MenuItem(8, itemDestroyPortal.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            MenuFactory.confirm(
                    getContext()!!,
                    langDestroyPortalConfirmTitle!!.str(),
                    itemDestroyPortalConfirmAccept.item(),
                    { player2: Player? ->
                        // Call event
                        val event = PortalDestroyEvent(player2!!, portal, false)
                        module!!.server.pluginManager.callEvent(event)
                        if (
                            event.isCancelled() && !player2.hasPermission(module!!.adminPermission)
                        ) {
                            module!!.langDestroyRestricted?.send(player2)
                            return@confirm Menu.ClickResult.ERROR
                        }

                        module!!.removePortal(portal)
                        Menu.ClickResult.SUCCESS
                    },
                    itemDestroyPortalConfirmCancel.item(),
                    { player2: Player? -> menu.open(player2!!) },
                )
                .tag(PortalMenuTag(portal.id()))
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Enables this menu component. */
    public override fun onEnable() {}

    /** Disables this menu component. */
    public override fun onDisable() {}
}
