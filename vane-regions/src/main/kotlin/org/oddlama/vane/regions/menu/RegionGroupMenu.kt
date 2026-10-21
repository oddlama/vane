package org.oddlama.vane.regions.menu

import java.util.stream.Collectors
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.EnvironmentSetting
import org.oddlama.vane.regions.region.RegionGroup
import org.oddlama.vane.regions.region.Role
import org.oddlama.vane.util.StorageUtil

@Suppress("UNUSED_PARAMETER")
/** Menu for managing one region group, its roles, and environment settings. */
class RegionGroupMenu(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("RegionGroup")) {
    @LangMessage
    /** Localized region-group menu title. */
    var langTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized confirmation title for region-group deletion. */
    var langDeleteConfirmTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for role selector menu. */
    var langSelectRoleTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for role filter input. */
    var langFilterRolesTitle: TranslatedMessage? = null

    /** Item for renaming the region group. */
    var itemRename: TranslatedItemStack<*>

    /** Item for deleting the region group. */
    var itemDelete: TranslatedItemStack<*>

    /** Item confirming region-group deletion. */
    var itemDeleteConfirmAccept: TranslatedItemStack<*>

    /** Item cancelling region-group deletion. */
    var itemDeleteConfirmCancel: TranslatedItemStack<*>

    /** Item opening role-creation flow. */
    var itemCreateRole: TranslatedItemStack<*>

    /** Item opening role list. */
    var itemListRoles: TranslatedItemStack<*>

    /** Item template for one role entry in selectors. */
    var itemSelectRole: TranslatedItemStack<*>

    /** Item shown for enabled setting toggle state. */
    var itemSettingToggleOn: TranslatedItemStack<*>

    /** Item shown for disabled setting toggle state. */
    var itemSettingToggleOff: TranslatedItemStack<*>

    /** Info item for the animals setting. */
    var itemSettingInfoAnimals: TranslatedItemStack<*>

    /** Info item for the monsters setting. */
    var itemSettingInfoMonsters: TranslatedItemStack<*>

    /** Info item for the explosions setting. */
    var itemSettingInfoExplosions: TranslatedItemStack<*>

    /** Info item for the fire setting. */
    var itemSettingInfoFire: TranslatedItemStack<*>

    /** Info item for the PVP setting. */
    var itemSettingInfoPvp: TranslatedItemStack<*>

    /** Info item for the trample setting. */
    var itemSettingInfoTrample: TranslatedItemStack<*>

    /** Info item for the vine-growth setting. */
    var itemSettingInfoVineGrowth: TranslatedItemStack<*>

    init {
        /** Shared menu context used to create translated item stacks. */
        val ctx = getContext()!!
        itemRename =
            TranslatedItemStack<Regions?>(
                ctx,
                "Rename",
                Material.NAME_TAG,
                1,
                "Used to rename the region group.",
            )
        itemDelete =
            TranslatedItemStack<Regions?>(
                ctx,
                "Delete",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to delete this region group.",
            )
        itemDeleteConfirmAccept =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmAccept",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to confirm deleting the region group.",
            )
        itemDeleteConfirmCancel =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmCancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel deleting the region group.",
            )
        itemCreateRole =
            TranslatedItemStack<Regions?>(
                ctx,
                "CreateRole",
                Material.WRITABLE_BOOK,
                1,
                "Used to create a new role.",
            )
        itemListRoles =
            TranslatedItemStack<Regions?>(
                ctx,
                "ListRoles",
                Material.GLOBE_BANNER_PATTERN,
                1,
                "Used to list all defined roles.",
            )
        itemSelectRole =
            TranslatedItemStack<Regions?>(
                ctx,
                "SelectRole",
                Material.GLOBE_BANNER_PATTERN,
                1,
                "Used to represent a role in the role selection list.",
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
        itemSettingInfoAnimals =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoAnimals",
                StorageUtil.namespacedKey("vane", "animals_baby_pig_2"),
                1,
                "Used to represent the info for the animals setting.",
            )
        itemSettingInfoMonsters =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoMonsters",
                Material.ZOMBIE_HEAD,
                1,
                "Used to represent the info for the monsters setting.",
            )
        itemSettingInfoExplosions =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoExplosions",
                StorageUtil.namespacedKey("vane", "monsters_creeper_with_tnt_2"),
                1,
                "Used to represent the info for the explosions setting.",
            )
        itemSettingInfoFire =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoFire",
                Material.CAMPFIRE,
                1,
                "Used to represent the info for the fire setting.",
            )
        itemSettingInfoPvp =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoPvP",
                Material.IRON_SWORD,
                1,
                "Used to represent the info for the pvp setting.",
            )
        itemSettingInfoTrample =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoTrample",
                Material.FARMLAND,
                1,
                "Used to represent the info for the trample setting.",
            )
        itemSettingInfoVineGrowth =
            TranslatedItemStack<Regions?>(
                ctx,
                "SettingInfoVineGrowth",
                Material.VINE,
                1,
                "Used to represent the info for the vine growth setting.",
            )
    }

    /** Creates and populates the region-group menu. */
    fun create(group: RegionGroup, player: Player): Menu {
        val columns = 9
        val rows = 3
        val title = langTitle!!.strComponent("§5§l" + (group.name() ?: ""))
        val regionGroupMenu =
            Menu(getContext()!!, Bukkit.createInventory(null, rows * columns, title))
        regionGroupMenu.tag(RegionGroupMenuTag(group.id()))

        val isOwner = player.uniqueId == group.owner()
        if (isOwner) {
            regionGroupMenu.add(menuItemRename(group))
            // Delete it only if this isn't the default group
            if (module!!.getOrCreateDefaultRegionGroup(player).id() != group.id()) {
                regionGroupMenu.add(menuItemDelete(group))
            }
        }

        regionGroupMenu.add(menuItemCreateRole(group))
        regionGroupMenu.add(menuItemListRoles(group))

        addMenuItemSetting(
            regionGroupMenu,
            group,
            0,
            itemSettingInfoAnimals,
            EnvironmentSetting.ANIMALS,
        )
        addMenuItemSetting(
            regionGroupMenu,
            group,
            1,
            itemSettingInfoMonsters,
            EnvironmentSetting.MONSTERS,
        )
        addMenuItemSetting(
            regionGroupMenu,
            group,
            3,
            itemSettingInfoExplosions,
            EnvironmentSetting.EXPLOSIONS,
        )
        addMenuItemSetting(regionGroupMenu, group, 4, itemSettingInfoFire, EnvironmentSetting.FIRE)
        addMenuItemSetting(regionGroupMenu, group, 5, itemSettingInfoPvp, EnvironmentSetting.PVP)
        addMenuItemSetting(
            regionGroupMenu,
            group,
            7,
            itemSettingInfoTrample,
            EnvironmentSetting.TRAMPLE,
        )
        addMenuItemSetting(
            regionGroupMenu,
            group,
            8,
            itemSettingInfoVineGrowth,
            EnvironmentSetting.VINE_GROWTH,
        )

        regionGroupMenu.onNaturalClose { player2: Player? ->
            player2?.let { module!!.menus?.mainMenu?.create(it)?.open(it) }
        }

        return regionGroupMenu
    }

    /** Builds the widget for renaming the region group. */
    private fun menuItemRename(group: RegionGroup): MenuWidget {
        return MenuItem(0, itemRename.item()) { player: Player?, menu: Menu?, self: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            module!!
                .menus
                ?.enterRegionGroupNameMenu
                ?.create(
                    p,
                    group.name() ?: "",
                ) { player2: Player?, name: String? ->
                    group.name(name)
                    markPersistentStorageDirty()

                    // Open new menu because of possibly changed title
                    player2?.let { module!!.menus?.regionGroupMenu?.create(group, it)?.open(it) }
                    Menu.ClickResult.SUCCESS
                }
                ?.onNaturalClose { player2: Player? ->
                    player2?.let {
                        module!!.menus?.regionGroupMenu?.create(group, it)?.open(it)
                    }
                }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for deleting the region group. */
    private fun menuItemDelete(group: RegionGroup): MenuWidget {
        val orphanCheckbox = if (group.isOrphan(module!!)) "§a✓" else "§c✕"
        return MenuItem(
            1,
            itemDelete.item(orphanCheckbox),
            Function3 { player: Player?, menu: Menu?, self: MenuItem? ->
                if (!group.isOrphan(module!!)) {
                    return@Function3 Menu.ClickResult.ERROR
                }
                val p = player!!
                menu!!.close(p)
                MenuFactory.confirm(
                        getContext()!!,
                        langDeleteConfirmTitle!!.str(),
                        itemDeleteConfirmAccept.item(),
                        { player2: Player? ->
                            if (player2 == null) return@confirm Menu.ClickResult.ERROR
                            if (player2.uniqueId != group.owner()) {
                                return@confirm Menu.ClickResult.ERROR
                            }
                            // Assert that this isn't the default group
                            if (
                                module!!.getOrCreateDefaultRegionGroup(player2).id() == group.id()
                            ) {
                                return@confirm Menu.ClickResult.ERROR
                            }

                            module!!.removeRegionGroup(group)
                            Menu.ClickResult.SUCCESS
                        },
                        itemDeleteConfirmCancel.item(),
                        { player2: Player? -> player2?.let { menu.open(it) } },
                    )
                    .tag(RegionGroupMenuTag(group.id()))
                    .open(p)
                Menu.ClickResult.SUCCESS
            },
        )
    }

    /** Builds the widget for creating a new role. */
    private fun menuItemCreateRole(group: RegionGroup): MenuWidget {
        return MenuItem(7, itemCreateRole.item()) { player: Player?, menu: Menu?, self: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            module!!
                .menus
                ?.enterRoleNameMenu
                ?.create(p) { player2: Player?, name: String? ->
                    val role = Role(name, Role.RoleType.NORMAL)
                    group.addRole(role)
                    markPersistentStorageDirty()
                    player2?.let { module!!.menus?.roleMenu?.create(group, role, it)?.open(it) }
                    Menu.ClickResult.SUCCESS
                }
                ?.onNaturalClose { player2: Player? -> player2?.let { menu.open(it) } }
                ?.open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for listing and selecting roles. */
    private fun menuItemListRoles(group: RegionGroup): MenuWidget {
        return MenuItem(8, itemListRoles.item()) { player: Player?, menu: Menu?, self: MenuItem? ->
            val p = player!!
            menu!!.close(p)
            val allRoles =
                group
                    .roles()
                    .stream()
                    .sorted { a: Role?, b: Role? ->
                        a!!.name()!!.compareTo(b!!.name()!!, ignoreCase = true)
                    }
                    .collect(Collectors.toList())

            val filter = nameFilter { r: Role? -> r?.name() }
            MenuFactory.genericSelector<Role?, Filter.StringFilter<Role?>?>(
                    getContext()!!,
                    p,
                    langSelectRoleTitle!!.str(),
                    langFilterRolesTitle!!.str(),
                    allRoles,
                    { r: Role? -> itemSelectRole.item(r!!.color() + "§l" + r.name()) },
                    filter,
                    { player2: Player?, m: Menu?, role: Role? ->
                        if (player2 == null || role == null) {
                            return@genericSelector Menu.ClickResult.ERROR
                        }
                        m!!.close(player2)
                        module!!.menus?.roleMenu?.create(group, role, player2)?.open(player2)
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> player2?.let { menu.open(it) } },
                )
                .open(p)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Adds a setting-info item and its associated toggle widget to the menu. */
    private fun addMenuItemSetting(
        regionGroupMenu: Menu,
        group: RegionGroup,
        col: Int,
        itemInfo: TranslatedItemStack<*>,
        setting: EnvironmentSetting,
    ) {
        regionGroupMenu.add(
            MenuItem(
                9 + col,
                itemInfo.item(),
            ) { player: Player?, menu: Menu?, self: MenuItem? ->
                Menu.ClickResult.IGNORE
            }
        )

        regionGroupMenu.add(
            menuItemSettingToggle(
                col,
                itemSettingToggleOn,
                itemSettingToggleOff,
                { setting.hasOverride() },
                { group.getSetting(setting) },
                {
                    group.settings()!![setting] = !group.getSetting(setting)
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
