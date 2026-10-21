package org.oddlama.vane.portals.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.menu.MenuItem
import org.oddlama.vane.core.menu.MenuWidget
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.util.StorageUtil

/**
 * Menu component providing settings UI for a portal (rename, icon, style, visibility, locks).
 *
 * The menu exposes actions and permission checks for players interacting with a portal's settings.
 *
 * @param context Module context used to resolve translations, configuration and subcomponents.
 * @constructor Creates a SettingsMenu module component using the provided module [context].
 */
class SettingsMenu(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.namespace("Settings")) {
    @LangMessage
    /** Translation for the settings menu title. */
    var langTitle: TranslatedMessage? = null

    @LangMessage
    /** Translation for the icon selection dialog title. */
    var langSelectIconTitle: TranslatedMessage? = null

    /** Item representing the rename action. */
    var itemRename: TranslatedItemStack<*>

    /** Item representing the icon selection action. */
    var itemSelectIcon: TranslatedItemStack<*>

    /** Item representing the style selection action. */
    var itemSelectStyle: TranslatedItemStack<*>

    /** Item indicating enabled exit-orientation lock. */
    var itemExitOrientationLockOn: TranslatedItemStack<*>

    /** Item indicating disabled exit-orientation lock. */
    var itemExitOrientationLockOff: TranslatedItemStack<*>

    /** Item for public visibility selection. */
    var itemVisibilityPublic: TranslatedItemStack<*>

    /** Item for group visibility selection. */
    var itemVisibilityGroup: TranslatedItemStack<*>

    /** Item for group-internal visibility selection. */
    var itemVisibilityGroupInternal: TranslatedItemStack<*>

    /** Item for private visibility selection. */
    var itemVisibilityPrivate: TranslatedItemStack<*>

    /** Item indicating enabled target lock. */
    var itemTargetLockOn: TranslatedItemStack<*>

    /** Item indicating disabled target lock. */
    var itemTargetLockOff: TranslatedItemStack<*>

    /** Item to go back to the previous menu. */
    var itemBack: TranslatedItemStack<*>

    init {
        val ctx = getContext()!!
        itemRename =
            TranslatedItemStack<Portals?>(
                ctx,
                "Rename",
                Material.NAME_TAG,
                1,
                "Used to rename the portal.",
            )
        itemSelectIcon =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectIcon",
                StorageUtil.namespacedKey("vane", "decoration_end_portal_orb"),
                1,
                "Used to select the portal's icon.",
            )
        itemSelectStyle =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectStyle",
                Material.ITEM_FRAME,
                1,
                "Used to change the portal's style.",
            )
        itemExitOrientationLockOn =
            TranslatedItemStack<Portals?>(
                ctx,
                "ExitOrientationLockOn",
                Material.SOUL_TORCH,
                1,
                "Used to toggle and indicate enabled exit orientation lock.",
            )
        itemExitOrientationLockOff =
            TranslatedItemStack<Portals?>(
                ctx,
                "ExitOrientationLockOff",
                Material.TORCH,
                1,
                "Used to toggle and indicate disabled exit orientation lock.",
            )
        itemVisibilityPublic =
            TranslatedItemStack<Portals?>(
                ctx,
                "VisibilityPublic",
                Material.ENDER_EYE,
                1,
                "Used to change and indicate public visibility.",
            )
        itemVisibilityGroup =
            TranslatedItemStack<Portals?>(
                ctx,
                "VisibilityGroup",
                Material.ENDER_PEARL,
                1,
                "Used to change and indicate group visibility.",
            )
        itemVisibilityGroupInternal =
            TranslatedItemStack<Portals?>(
                ctx,
                "VisibilityGroupInternal",
                Material.FIRE_CHARGE,
                1,
                "Used to change and indicate group internal visibility.",
            )
        itemVisibilityPrivate =
            TranslatedItemStack<Portals?>(
                ctx,
                "VisibilityPrivate",
                Material.FIREWORK_STAR,
                1,
                "Used to change and indicate private visibility.",
            )
        itemTargetLockOn =
            TranslatedItemStack<Portals?>(
                ctx,
                "TargetLockOn",
                Material.SLIME_BALL,
                1,
                "Used to toggle and indicate enabled target lock.",
            )
        itemTargetLockOff =
            TranslatedItemStack<Portals?>(
                ctx,
                "TargetLockOff",
                Material.SNOWBALL,
                1,
                "Used to toggle and indicate disabled target lock.",
            )
        itemBack =
            TranslatedItemStack<Portals?>(
                ctx,
                "Back",
                Material.PRISMARINE_SHARD,
                1,
                "Used to go back to the previous menu.",
            )
    }

    /**
     * Helper: centralize settings-change permission/event check and notify player if blocked.
     *
     * This will fire a [PortalChangeSettingsEvent] and, if cancelled and the player lacks admin
     * permission, send the configured restricted message to the player.
     *
     * @param player The player attempting to change the portal settings.
     * @param portal The portal whose settings would be changed.
     * @return True when the change is allowed (or player has admin permission), false otherwise.
     */
    private fun checkSettingsOrNotify(player: Player, portal: Portal): Boolean {
        val module = module ?: return false
        val evt = PortalChangeSettingsEvent(player, portal, false)
        module.server.pluginManager.callEvent(evt)
        if (evt.isCancelled && !player.hasPermission(module.adminPermission)) {
            module.langSettingsRestricted?.send(player)
            return false
        }
        return true
    }

    // HINT: We don't capture the previous menu and open a new one on exit,
    // to correctly reflect changes done in here. (e.g., menu title due to portal name)
    @Suppress("UNUSED_PARAMETER")
    /**
     * Build and return the settings [Menu] for the given portal and player.
     *
     * The returned menu is tagged with the portal id and contains items to rename the portal,
     * select an icon, choose a style, toggle orientation/target locks and change visibility.
     *
     * @param portal Portal to edit.
     * @param player The player opening the menu (may be null when opened programmatically).
     * @param console Optional console block associated with the portal (may be null).
     * @return A configured [Menu] instance for editing the provided portal.
     */
    fun create(portal: Portal, player: Player?, console: Block?): Menu {
        val columns = 9
        val title = langTitle!!.strComponent("§5§l${portal.name()}")
        val settingsMenu = Menu(getContext()!!, Bukkit.createInventory(null, columns, title))
        settingsMenu.tag(PortalMenuTag(portal.id()))

        settingsMenu.add(menuItemRename(portal, console))
        settingsMenu.add(menuItemSelectIcon(portal))
        settingsMenu.add(menuItemSelectStyle(portal))
        settingsMenu.add(menuItemExitOrientationLock(portal))
        settingsMenu.add(menuItemVisibility(portal))
        settingsMenu.add(menuItemTargetLock(portal))
        settingsMenu.add(menuItemBack(portal, console))

        settingsMenu.onNaturalClose { player2: Player? ->
            val module = module ?: return@onNaturalClose
            player2?.let { p -> module.menus?.consoleMenu?.create(portal, p, console)?.open(p) }
        }

        return settingsMenu
    }

    /**
     * Create the "rename" menu widget for the provided [portal].
     *
     * When activated this widget will close the current menu and open the configured enter-name
     * menu. If the player confirms the new name and passes permission/event checks the portal name
     * is updated and the settings menu is reopened.
     *
     * @param portal Portal being edited.
     * @param console Optional console block associated with the portal (passed to reopened menus).
     * @return A [MenuWidget] that implements the rename action.
     */
    private fun menuItemRename(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(0, itemRename.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val module = module ?: return@MenuItem Menu.ClickResult.ERROR
            val p = player ?: return@MenuItem Menu.ClickResult.ERROR
            val currentMenu = menu ?: return@MenuItem Menu.ClickResult.ERROR
            currentMenu.close(p)

            // Ensure enterNameMenu exists; chain calls safely because menus may be nullable
            module.menus
                ?.enterNameMenu
                ?.create(
                    p,
                    portal.name() ?: "",
                    Function2 { player2: Player?, name: String? ->
                        val p2 = player2 ?: return@Function2 Menu.ClickResult.ERROR
                        if (!checkSettingsOrNotify(p2, portal))
                            return@Function2 Menu.ClickResult.ERROR

                        portal.name(name)

                        // Update portal icons to reflect new name
                        module.updatePortalIcon(portal)

                        // Open new menu because of possibly changed title
                        module.menus?.settingsMenu?.create(portal, p2, console)?.open(p2)
                        Menu.ClickResult.SUCCESS
                    },
                )
                ?.onNaturalClose { player2: Player? ->
                    player2?.let { q ->
                        module.menus?.settingsMenu?.create(portal, q, console)?.open(q)
                    }
                }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /**
     * Create the "select icon" menu widget for the provided [portal].
     *
     * Opens an item selector allowing the player to pick a new icon. After selection
     * permission/event checks are performed and the portal icon is updated.
     *
     * @param portal Portal being edited.
     * @return A [MenuWidget] that opens the item selector for icon selection.
     */
    private fun menuItemSelectIcon(portal: Portal): MenuWidget {
        return MenuItem(1, itemSelectIcon.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val module = module ?: return@MenuItem Menu.ClickResult.ERROR
            val p = player ?: return@MenuItem Menu.ClickResult.ERROR
            val currentMenu = menu ?: return@MenuItem Menu.ClickResult.ERROR
            currentMenu.close(p)

            MenuFactory.itemSelector(
                    getContext()!!,
                    p,
                    langSelectIconTitle!!.str(),
                    portal.icon(),
                    true,
                    { player2: Player?, item: ItemStack? ->
                        val p2 = player2 ?: return@itemSelector Menu.ClickResult.ERROR
                        if (!checkSettingsOrNotify(p2, portal))
                            return@itemSelector Menu.ClickResult.ERROR

                        portal.icon(item)
                        module.updatePortalIcon(portal)
                        currentMenu.open(p2)
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> player2?.let(currentMenu::open) },
                )
                .tag(PortalMenuTag(portal.id()))
                .open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /**
     * Create the "select style" menu widget for the provided [portal].
     *
     * When activated this will perform permission/event checks and open the style menu where the
     * player can change the portal's visual style.
     *
     * @param portal Portal being edited.
     * @return A [MenuWidget] that opens the style selection menu.
     */
    private fun menuItemSelectStyle(portal: Portal): MenuWidget {
        return MenuItem(
            2,
            itemSelectStyle.item(),
            Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                val module = module ?: return@Function3 Menu.ClickResult.ERROR
                val p = player ?: return@Function3 Menu.ClickResult.ERROR
                if (!checkSettingsOrNotify(p, portal)) return@Function3 Menu.ClickResult.ERROR

                val currentMenu = menu ?: return@Function3 Menu.ClickResult.ERROR
                currentMenu.close(p)
                val mg = module.menus ?: return@Function3 Menu.ClickResult.ERROR
                val sm = mg.styleMenu ?: return@Function3 Menu.ClickResult.ERROR
                sm.create(portal, p, currentMenu).open(p)
                Menu.ClickResult.SUCCESS
            },
        )
    }

    /**
     * Create the menu widget that toggles the portal's exit-orientation lock.
     *
     * Left-clicking toggles the lock (after permission/event checks). The widget's displayed item
     * reflects the current lock state.
     *
     * @param portal Portal being edited.
     * @return A [MenuWidget] that toggles exit-orientation lock and updates its icon.
     */
    private fun menuItemExitOrientationLock(portal: Portal): MenuWidget {
        return object :
            MenuItem(
                4,
                null,
                Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                    if (player == null || !checkSettingsOrNotify(player, portal))
                        return@Function3 Menu.ClickResult.ERROR

                    portal.exitOrientationLocked(!portal.exitOrientationLocked())
                    menu!!.update()
                    Menu.ClickResult.SUCCESS
                },
            ) {
            /**
             * Update the displayed item for the lock toggle based on the portal state.
             *
             * @param item Ignored; replaced with the appropriate on/off item.
             */
            override fun item(item: ItemStack?) {
                if (portal.exitOrientationLocked()) {
                    super.item(itemExitOrientationLockOn.item())
                } else {
                    super.item(itemExitOrientationLockOff.item())
                }
            }
        }
    }

    /**
     * Create the visibility toggle menu widget for the provided [portal].
     *
     * Left/right-clicking cycles through visibility modes. If the "regions" plugin is not
     * installed, group-based visibility modes are skipped. Permission checks and events are
     * performed before applying changes.
     *
     * @param portal Portal being edited.
     * @return A [MenuWidget] that cycles and displays portal visibility.
     */
    private fun menuItemVisibility(portal: Portal): MenuWidget {
        return object :
            MenuItem(
                5,
                null,
                Function4 { player: Player?, menu: Menu?, _: MenuItem?, event: InventoryClickEvent?
                    ->
                    val module = module ?: return@Function4 Menu.ClickResult.ERROR
                    if (!Menu.isLeftOrRightClick(event)) {
                        return@Function4 Menu.ClickResult.INVALID_CLICK
                    }
                    val p = player ?: return@Function4 Menu.ClickResult.ERROR
                    if (!checkSettingsOrNotify(p, portal)) return@Function4 Menu.ClickResult.ERROR
                    val currentMenu = menu ?: return@Function4 Menu.ClickResult.ERROR
                    val clickType = event?.click ?: return@Function4 Menu.ClickResult.INVALID_CLICK

                    var newVis = portal.visibility() ?: Portal.Visibility.PRIVATE
                    // If the "regions" plugin is not installed, we need to skip group visibility.
                    do {
                        newVis = if (clickType == ClickType.RIGHT) newVis.prev() else newVis.next()
                    } while (newVis.requiresRegions() && !module.isRegionsInstalled)

                    portal.visibility(newVis)
                    module.updatePortalVisibility(portal)
                    currentMenu.update()
                    Menu.ClickResult.SUCCESS
                },
            ) {
            /**
             * Update the displayed item to represent the portal's current visibility.
             *
             * @param item Ignored; replaced with the appropriate visibility item.
             */
            override fun item(item: ItemStack?) {
                when (portal.visibility() ?: Portal.Visibility.PRIVATE) {
                    Portal.Visibility.PUBLIC -> super.item(itemVisibilityPublic.item())
                    Portal.Visibility.GROUP -> super.item(itemVisibilityGroup.item())
                    Portal.Visibility.GROUP_INTERNAL ->
                        super.item(itemVisibilityGroupInternal.item())
                    Portal.Visibility.PRIVATE -> super.item(itemVisibilityPrivate.item())
                }
            }
        }
    }

    /**
     * Create the menu widget that toggles the portal's target lock.
     *
     * Left-clicking toggles the lock (after permission/event checks). The widget's displayed item
     * reflects whether the target lock is enabled or disabled.
     *
     * @param portal Portal being edited.
     * @return A [MenuWidget] that toggles target lock and updates its icon.
     */
    private fun menuItemTargetLock(portal: Portal): MenuWidget {
        return object :
            MenuItem(
                6,
                null,
                Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                    if (player == null || !checkSettingsOrNotify(player, portal))
                        return@Function3 Menu.ClickResult.ERROR

                    portal.targetLocked(!portal.targetLocked())
                    menu!!.update()
                    Menu.ClickResult.SUCCESS
                },
            ) {
            /**
             * Update the displayed item for the target lock toggle based on the portal state.
             *
             * @param item Ignored; replaced with the appropriate on/off item.
             */
            override fun item(item: ItemStack?) {
                if (portal.targetLocked()) {
                    super.item(itemTargetLockOn.item())
                } else {
                    super.item(itemTargetLockOff.item())
                }
            }
        }
    }

    /**
     * Create the "back" menu widget which returns the player to the console menu.
     *
     * When activated the current menu is closed and the console menu for the portal is opened for
     * the player.
     *
     * @param portal Portal being edited.
     * @param console Optional console block associated with the portal.
     * @return A [MenuWidget] that navigates back to the console menu.
     */
    private fun menuItemBack(portal: Portal, console: Block?): MenuWidget {
        return MenuItem(8, itemBack.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            val module = module ?: return@MenuItem Menu.ClickResult.ERROR
            val p = player ?: return@MenuItem Menu.ClickResult.ERROR
            val currentMenu = menu ?: return@MenuItem Menu.ClickResult.ERROR
            currentMenu.close(p)
            module.menus?.consoleMenu?.create(portal, p, console)?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /**
     * Called when this module component is enabled.
     *
     * This implementation does nothing but is provided to satisfy the lifecycle.
     */
    public override fun onEnable() {}

    /**
     * Called when this module component is disabled.
     *
     * This implementation does nothing but is provided to satisfy the lifecycle.
     */
    public override fun onDisable() {}
}
