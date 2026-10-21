package org.oddlama.vane.regions.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.RegionGroup
import org.oddlama.vane.regions.region.Role
import org.oddlama.vane.regions.region.RoleSetting
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil

@Suppress("UNUSED_PARAMETER")
/** Menu for managing one role, its members, and role-level permission settings. */
class RoleMenu(context: Context<Regions?>) : ModuleComponent<Regions?>(context.namespace("Role")) {
    @LangMessage
    /** Localized role menu title. */
    var langTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized confirmation title for role deletion. */
    var langDeleteConfirmTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for assign-player selector. */
    var langSelectAssignPlayerTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for remove-player selector. */
    var langSelectRemovePlayerTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for player filter input. */
    var langFilterPlayersTitle: TranslatedMessage? = null

    /** Item for renaming the role. */
    var itemRename: TranslatedItemStack<*>

    /** Item for deleting the role. */
    var itemDelete: TranslatedItemStack<*>

    /** Item confirming role deletion. */
    var itemDeleteConfirmAccept: TranslatedItemStack<*>

    /** Item cancelling role deletion. */
    var itemDeleteConfirmCancel: TranslatedItemStack<*>

    /** Item opening player-assignment flow. */
    var itemAssignPlayer: TranslatedItemStack<*>

    /** Item opening player-removal flow. */
    var itemRemovePlayer: TranslatedItemStack<*>

    /** Item template for one player entry in selectors. */
    var itemSelectPlayer: TranslatedItemStack<*>

    /** Item shown for enabled setting toggle state. */
    var itemSettingToggleOn: TranslatedItemStack<*>

    /** Item shown for disabled setting toggle state. */
    var itemSettingToggleOff: TranslatedItemStack<*>

    /** Info item for the admin setting. */
    var itemSettingInfoAdmin: TranslatedItemStack<*>

    /** Info item for the build setting. */
    var itemSettingInfoBuild: TranslatedItemStack<*>

    /** Info item for the use setting. */
    var itemSettingInfoUse: TranslatedItemStack<*>

    /** Info item for the container setting. */
    var itemSettingInfoContainer: TranslatedItemStack<*>

    /** Info item for the portal setting. */
    var itemSettingInfoPortal: TranslatedItemStack<*>

    init {
        /** Shared menu context used to create translated item stacks. */
        val ctx = getContext()!!
        itemRename =
            TranslatedItemStack<Regions?>(
                ctx,
                "Rename",
                Material.NAME_TAG,
                1,
                "Used to rename the role.",
            )
        itemDelete =
            TranslatedItemStack<Regions?>(
                ctx,
                "Delete",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to delete this role.",
            )
        itemDeleteConfirmAccept =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmAccept",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to confirm deleting the role.",
            )
        itemDeleteConfirmCancel =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmCancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel deleting the role.",
            )
        itemAssignPlayer =
            TranslatedItemStack<Regions?>(
                ctx,
                "AssignPlayer",
                Material.PLAYER_HEAD,
                1,
                "Used to assign players to this role.",
            )
        itemRemovePlayer =
            TranslatedItemStack<Regions?>(
                ctx,
                "RemovePlayer",
                Material.PLAYER_HEAD,
                1,
                "Used to remove players from this role.",
            )
        itemSelectPlayer =
            TranslatedItemStack<Regions?>(
                ctx,
                "SelectPlayer",
                Material.PLAYER_HEAD,
                1,
                "Used to represent a player in the role assignment/removal list.",
            )

        itemSettingToggleOn =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingToggleOn",
                Material.GREEN_TERRACOTTA,
                1,
                "Used to represent a toggle button with current state on.",
            )
        itemSettingToggleOff =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingToggleOff",
                Material.RED_TERRACOTTA,
                1,
                "Used to represent a toggle button with current state off.",
            )
        itemSettingInfoAdmin =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoAdmin",
                Material.GOLDEN_APPLE,
                1,
                "Used to represent the info for the admin setting.",
            )
        itemSettingInfoBuild =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoBuild",
                Material.IRON_PICKAXE,
                1,
                "Used to represent the info for the build setting.",
            )
        itemSettingInfoUse =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoUse",
                Material.DARK_OAK_DOOR,
                1,
                "Used to represent the info for the use setting.",
            )
        itemSettingInfoContainer =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoContainer",
                Material.CHEST,
                1,
                "Used to represent the info for the container setting.",
            )

        itemSettingInfoPortal =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoPortal",
                Material.ENDER_PEARL,
                1,
                "Used to represent the info for the portal setting.",
            )
    }

    /** Creates and populates the role menu. */
    fun create(group: RegionGroup, role: Role, player: Player): Menu {
        val columns = 9
        val rows = 3
        val title = langTitle!!.strComponent(role.color() + "§l" + role.name())
        val roleMenu = Menu(getContext()!!, Bukkit.createInventory(null, rows * columns, title))

        val isAdmin =
            player.uniqueId == group.owner() ||
                group.getRole(player.uniqueId)!!.getSetting(RoleSetting.ADMIN)

        if (isAdmin && role.roleType() == Role.RoleType.NORMAL) {
            roleMenu.add(menuItemRename(group, role))
            roleMenu.add(menuItemDelete(group, role))
        }

        if (role.roleType() != Role.RoleType.OTHERS) {
            roleMenu.add(menuItemAssignPlayer(group, role))
            roleMenu.add(menuItemRemovePlayer(group, role))
        }

        addMenuItemSetting(roleMenu, role, 0, itemSettingInfoAdmin, RoleSetting.ADMIN)
        addMenuItemSetting(roleMenu, role, 2, itemSettingInfoBuild, RoleSetting.BUILD)
        addMenuItemSetting(roleMenu, role, 4, itemSettingInfoUse, RoleSetting.USE)
        addMenuItemSetting(roleMenu, role, 5, itemSettingInfoContainer, RoleSetting.CONTAINER)

        if (module!!.vanePortalsAvailable) {
            addMenuItemSetting(roleMenu, role, 8, itemSettingInfoPortal, RoleSetting.PORTAL)
        }

        roleMenu.onNaturalClose { player2: Player? ->
            player2?.let { module!!.menus?.regionGroupMenu?.create(group, it)?.open(it) }
        }

        return roleMenu
    }

    /** Builds the widget for renaming a role. */
    private fun menuItemRename(group: RegionGroup, role: Role): MenuWidget {
        return MenuItem(0, itemRename.item()) { player: Player?, menu: Menu?, self: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            module!!
                .menus
                ?.enterRoleNameMenu
                ?.create(p, role.name() ?: "") { player2: Player?, name: String? ->
                    role.name(name)
                    markPersistentStorageDirty()

                    // Open new menu because of possibly changed title
                    player2?.let { module!!.menus?.roleMenu?.create(group, role, it)?.open(it) }
                    Menu.ClickResult.SUCCESS
                }
                ?.onNaturalClose { player2: Player? ->
                    player2?.let {
                        module!!.menus?.roleMenu?.create(group, role, it)?.open(it)
                    }
                }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for deleting a role. */
    private fun menuItemDelete(group: RegionGroup, role: Role): MenuWidget {
        return MenuItem(1, itemDelete.item()) { player: Player?, menu: Menu?, self: MenuItem? ->
            menu!!.close(player!!)
            MenuFactory.confirm(
                    getContext()!!,
                    langDeleteConfirmTitle!!.str(),
                    itemDeleteConfirmAccept.item(),
                    { player2: Player? ->
                        group.removeRole(role.id()!!)
                        markPersistentStorageDirty()
                        Menu.ClickResult.SUCCESS
                    },
                    itemDeleteConfirmCancel.item(),
                    { player2: Player? -> player2?.let { menu.open(it) } },
                )
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for assigning players to this role. */
    private fun menuItemAssignPlayer(group: RegionGroup, role: Role): MenuWidget {
        return MenuItem(7, itemAssignPlayer.item()) { player: Player?, menu: Menu?, self: MenuItem?
            ->
            menu!!.close(player!!)
            val (allPlayers, filter) = sortedOfflinePlayers()
            MenuFactory.genericSelector<OfflinePlayer?, Filter.StringFilter<OfflinePlayer?>?>(
                    getContext()!!,
                    player,
                    langSelectAssignPlayerTitle!!.str(),
                    langFilterPlayersTitle!!.str(),
                    allPlayers,
                    { p: OfflinePlayer? ->
                        itemSelectPlayer.alternative(
                            ItemUtil.skullForPlayer(p, true),
                            "§a§l" + p!!.name,
                        )
                    },
                    filter,
                    { player2: Player?, m: Menu?, p: OfflinePlayer? ->
                        allPlayers.remove(p)
                        m!!.update()
                        group.playerToRole()!![p!!.uniqueId] = role.id()
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> player2?.let { menu.open(it) } },
                )
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for removing players from this role. */
    private fun menuItemRemovePlayer(group: RegionGroup, role: Role): MenuWidget {
        return MenuItem(8, itemRemovePlayer.item()) { player: Player?, menu: Menu?, self: MenuItem?
            ->
            menu!!.close(player!!)
            val (allPlayers, filter) = sortedOfflinePlayers()
            MenuFactory.genericSelector<OfflinePlayer?, Filter.StringFilter<OfflinePlayer?>?>(
                    getContext()!!,
                    player,
                    langSelectRemovePlayerTitle!!.str(),
                    langFilterPlayersTitle!!.str(),
                    allPlayers,
                    { p: OfflinePlayer? ->
                        itemSelectPlayer.alternative(
                            ItemUtil.skullForPlayer(p, true),
                            "§a§l" + p!!.name,
                        )
                    },
                    filter,
                    { player2: Player?, m: Menu?, p: OfflinePlayer? ->
                        allPlayers.remove(p)
                        m!!.update()
                        group.playerToRole()!!.remove(p!!.uniqueId)
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> player2?.let { menu.open(it) } },
                )
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Adds a setting-info item and its associated toggle widget to the menu. */
    private fun addMenuItemSetting(
        roleMenu: Menu,
        role: Role,
        col: Int,
        itemInfo: TranslatedItemStack<*>,
        setting: RoleSetting,
    ) {
        roleMenu.add(
            MenuItem(
                9 + col,
                itemInfo.item(),
            ) { player: Player?, menu: Menu?, self: MenuItem? ->
                Menu.ClickResult.IGNORE
            }
        )

        roleMenu.add(
            menuItemSettingToggle(
                col,
                itemSettingToggleOn,
                itemSettingToggleOff,
                { setting.hasOverride() || setting == RoleSetting.ADMIN },
                { role.getSetting(setting) },
                {
                    role.settings()!![setting] = !role.getSetting(setting)
                    markPersistentStorageDirty()
                },
            )
        )
    }

    /** No-op lifecycle hook. */
    override fun onEnable() {}

    /** No-op lifecycle hook. */
    override fun onDisable() {}
}
