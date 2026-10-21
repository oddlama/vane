package org.oddlama.vane.regions.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.Region
import org.oddlama.vane.regions.region.RegionGroup
import org.oddlama.vane.regions.region.RegionSelection
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil

/** Menu for managing one region, including rename, deletion, and group assignment. */
class RegionMenu(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("Region")) {
    @LangMessage
    /** Localized region menu title. */
    var langTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized confirmation title for region deletion. */
    var langDeleteConfirmTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for region-group selector menu. */
    var langSelectRegionGroupTitle: TranslatedMessage? = null

    @LangMessage
    /** Localized title for region-group filter input. */
    var langFilterRegionGroupsTitle: TranslatedMessage? = null

    /** Item for renaming the region. */
    var itemRename: TranslatedItemStack<*>

    /** Item for deleting the region. */
    var itemDelete: TranslatedItemStack<*>

    /** Item confirming region deletion. */
    var itemDeleteConfirmAccept: TranslatedItemStack<*>

    /** Item cancelling region deletion. */
    var itemDeleteConfirmCancel: TranslatedItemStack<*>

    /** Item opening region-group assignment flow. */
    var itemAssignRegionGroup: TranslatedItemStack<*>

    /** Item template for one region-group entry in assignment selectors. */
    var itemSelectRegionGroup: TranslatedItemStack<*>

    init {
        /** Shared menu context used to create translated item stacks. */
        val ctx = getContext()!!
        itemRename =
            TranslatedItemStack<Regions?>(
                ctx,
                "Rename",
                Material.NAME_TAG,
                1,
                "Used to rename the region.",
            )
        itemDelete =
            TranslatedItemStack<Regions?>(
                ctx,
                "Delete",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to delete this region.",
            )
        itemDeleteConfirmAccept =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmAccept",
                StorageUtil.namespacedKey("vane", "decoration_tnt_1"),
                1,
                "Used to confirm deleting the region.",
            )
        itemDeleteConfirmCancel =
            TranslatedItemStack<Regions?>(
                ctx,
                "DeleteConfirmCancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel deleting the region.",
            )
        itemAssignRegionGroup =
            TranslatedItemStack<Regions?>(
                ctx,
                "AssignRegionGroup",
                Material.GLOBE_BANNER_PATTERN,
                1,
                "Used to assign a region group.",
            )
        itemSelectRegionGroup =
            TranslatedItemStack<Regions?>(
                ctx,
                "SelectRegionGroup",
                Material.GLOBE_BANNER_PATTERN,
                1,
                "Used to represent a region group in the region group assignment list.",
            )
    }

    /** Creates and populates the region menu. */
    fun create(region: Region, player: Player): Menu {
        val columns = 9
        val title = langTitle!!.strComponent("§5§l" + region.name())
        val regionMenu = Menu(getContext()!!, Bukkit.createInventory(null, columns, title))
        regionMenu.tag(RegionMenuTag(region.id()))

        if (module!!.mayAdministrate(player, region)) {
            regionMenu.add(menuItemRename(region))
            regionMenu.add(menuItemDelete(region))
            regionMenu.add(menuItemAssignRegionGroup(region))
        }

        regionMenu.onNaturalClose { player2: Player? ->
            player2?.let { p -> module!!.menus?.mainMenu?.create(p)?.open(p) }
        }

        return regionMenu
    }

    /** Builds the widget for renaming the region. */
    private fun menuItemRename(region: Region): MenuWidget {
        return MenuItem(
            0,
            itemRename.item(),
            Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                val p = player!!
                menu!!.close(p)
                if (!module!!.mayAdministrate(p, region)) {
                    return@Function3 Menu.ClickResult.ERROR
                }

                module!!
                    .menus
                    ?.enterRegionNameMenu
                    ?.create(
                        p,
                        region.name() ?: "",
                        Function2 { player2: Player?, name: String? ->
                            if (player2 == null) return@Function2 Menu.ClickResult.ERROR
                            region.name(name)
                            // Update map marker
                            module!!.updateMarker(region)

                            // Open new menu because of possibly changed title
                            player2.let { module!!.menus?.regionMenu?.create(region, it)?.open(it) }
                            Menu.ClickResult.SUCCESS
                        },
                    )
                    ?.onNaturalClose { player2: Player? ->
                        player2?.let { module!!.menus?.regionMenu?.create(region, it)?.open(it) }
                    }
                    ?.open(p)
                Menu.ClickResult.SUCCESS
            },
        )
    }

    /** Builds the widget for deleting the region and refunding cost. */
    private fun menuItemDelete(region: Region): MenuWidget {
        return MenuItem(1, itemDelete.item()) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            MenuFactory.confirm(
                    getContext()!!,
                    langDeleteConfirmTitle!!.str(),
                    itemDeleteConfirmAccept.item(),
                    { player2: Player? ->
                        if (player2 == null || !module!!.mayAdministrate(player2, region)) {
                            return@confirm Menu.ClickResult.ERROR
                        }
                        module!!.removeRegion(region)

                        // Give back money
                        val tempSel = RegionSelection(module!!)
                        tempSel.primary = region.extent()!!.min()
                        tempSel.secondary = region.extent()!!.max()

                        val price = tempSel.price()
                        if (module!!.configEconomyAsCurrency) {
                            val success = module!!.economy?.deposit(player2, price) ?: false
                            if (!success) {
                                module!!
                                    .log
                                    .severe(
                                        "Player " +
                                            player2 +
                                            " deleted region '" +
                                            region.name() +
                                            "' (cost " +
                                            price +
                                            ") but the economy plugin failed to deposit."
                                    )
                            }
                        } else {
                            val currency = module!!.configCurrency ?: Material.DIAMOND
                            PlayerUtil.giveItems(player2, ItemStack(currency), price.toInt())
                        }
                        Menu.ClickResult.SUCCESS
                    },
                    itemDeleteConfirmCancel.item(),
                    { player2: Player? -> menu.open(player2!!) },
                )
                .tag(RegionMenuTag(region.id()))
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Builds the widget for assigning the region to another region group. */
    private fun menuItemAssignRegionGroup(region: Region): MenuWidget {
        return MenuItem(2, itemAssignRegionGroup.item()) {
            player: Player?,
            menu: Menu?,
            _: MenuItem? ->
            menu!!.close(player!!)
            val (allRegionGroups, filter) = module!!.administrableRegionGroups(player)
            MenuFactory.genericSelector<RegionGroup?, Filter.StringFilter<RegionGroup?>?>(
                    getContext()!!,
                    player,
                    langSelectRegionGroupTitle!!.str(),
                    langFilterRegionGroupsTitle!!.str(),
                    allRegionGroups,
                    { r: RegionGroup? -> itemSelectRegionGroup.item("§a§l" + r!!.name()) },
                    filter,
                    { player2: Player?, m: Menu?, group: RegionGroup? ->
                        if (player2 == null || !module!!.mayAdministrate(player2, region)) {
                            return@genericSelector Menu.ClickResult.ERROR
                        }
                        m!!.close(player2)
                        region.regionGroupId(group!!.id())
                        markPersistentStorageDirty()
                        menu.open(player2)
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> menu.open(player2!!) },
                )
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** No-op lifecycle hook. */
    override fun onEnable() {}

    /** No-op lifecycle hook. */
    override fun onDisable() {}
}
