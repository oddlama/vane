package org.oddlama.vane.core.menu

import kotlin.random.Random
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.*
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.material.HeadMaterialLibrary.all
import org.oddlama.vane.core.menu.Menu.Companion.isLeftClick
import org.oddlama.vane.core.menu.Menu.Companion.isLeftOrRightClick
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil

/** Factory helpers for constructing common menu patterns. */
object MenuFactory {
    /** Creates an anvil-based string input menu. */
    @JvmStatic
    fun anvilStringInput(
        context: Context<*>,
        player: Player,
        title: String,
        inputItemStack: ItemStack,
        defaultName: String,
        onClick: Function3<Player?, Menu?, String?, Menu.ClickResult?>,
    ): Menu {
        val inputItem = inputItemStack.clone()
        val meta = inputItem.itemMeta
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(defaultName))
        inputItem.itemMeta = meta

        val anvil = AnvilMenu(context, player, title)
        anvil.add(MenuItem(0, inputItem))
        anvil.add(
            MenuItemClickListener(2) { p: Player?, menu: Menu?, item: ItemStack? ->
                onClick.apply(
                    p,
                    menu,
                    ItemUtil.nameOf(item),
                )
            }
        )
        return anvil
    }

    /** Creates a one-row randomized confirm/cancel menu. */
    @JvmStatic
    fun confirm(
        context: Context<*>,
        title: String,
        itemConfirm: ItemStack?,
        onConfirm: Function1<Player?, Menu.ClickResult?>,
        itemCancel: ItemStack?,
        onCancel: Consumer1<Player?>,
    ): Menu {
        val columns = 9
        val confirmationMenu =
            Menu(
                context,
                Bukkit.createInventory(
                    null,
                    columns,
                    LegacyComponentSerializer.legacySection().deserialize(title),
                ),
            )
        val confirmIndex = Random.nextInt(columns)

        for (i in 0 until columns) {
            if (i == confirmIndex) {
                confirmationMenu.add(
                    MenuItem(i, itemConfirm) { p, menu, _ ->
                        menu!!.close(p!!)
                        onConfirm.apply(p)
                    }
                )
            } else {
                confirmationMenu.add(
                    MenuItem(i, itemCancel) { p, menu, _ ->
                        menu!!.close(p!!)
                        onCancel.apply(p)
                        Menu.ClickResult.SUCCESS
                    }
                )
            }
        }

        // On natural close call cancel
        confirmationMenu.onNaturalClose(onCancel)

        return confirmationMenu
    }

    /** Creates an item selector menu with optional item post-processing. */
    @JvmStatic
    @JvmOverloads
    fun itemSelector(
        context: Context<*>,
        player: Player?,
        title: String,
        initialItem: ItemStack?,
        allowNothing: Boolean,
        onConfirm: Function2<Player?, ItemStack?, Menu.ClickResult?>,
        onCancel: Consumer1<Player?>,
        onSelectItem: Function1<ItemStack?, ItemStack?> = Function1 { it },
    ): Menu {
        // Use non-null assertion: menuManager is expected to be initialized when menus are created
        val menuManager = context.module!!.core!!.menuManager!!
        val setItemName: Function1<ItemStack?, ItemStack?> = Function1 { item: ItemStack? ->
            ItemUtil.nameItem(
                item!!,
                menuManager.itemSelectorSelected!!.langName!!.format(),
                menuManager.itemSelectorSelected!!.langLore!!.format(),
            )
        }

        val noItem = setItemName.apply(ItemStack(Material.BARRIER))
        val defaultItem: ItemStack? =
            if (initialItem == null || initialItem.type == Material.AIR) {
                noItem
            } else {
                initialItem
            }

        val columns = 9
        val itemSelectorMenu =
            Menu(
                context,
                Bukkit.createInventory(
                    null,
                    columns,
                    LegacyComponentSerializer.legacySection().deserialize(title),
                ),
            )
        val selectedItem =
            object :
                MenuItem(
                    4,
                    defaultItem,
                    Function4 {
                        _: Player?,
                        menu: Menu?,
                        self: MenuItem?,
                        event: InventoryClickEvent? ->
                        if (!isLeftOrRightClick(event)) {
                            return@Function4 Menu.ClickResult.INVALID_CLICK
                        }
                        if (allowNothing && event!!.click == ClickType.RIGHT) {
                            // Clear selection
                            self!!.updateItem(menu!!, noItem)
                        } else {
                            // Reset selection
                            self!!.updateItem(menu!!, defaultItem)
                        }
                        Menu.ClickResult.SUCCESS
                    },
                ) {
                var originalSelected: ItemStack? = null

                override fun item(item: ItemStack?) {
                    this.originalSelected = item
                    if (item != null) {
                        super.item(setItemName.apply(item.clone()))
                    } else {
                        super.item(null as ItemStack?)
                    }
                }
            }

        // Selected item, begin with default selected
        selectedItem.item(defaultItem)
        itemSelectorMenu.add(selectedItem)

        // Inventory listener
        itemSelectorMenu.add(
            MenuItemClickListener(
                -1,
                Function3 { _: Player?, menu: Menu?, raw: ItemStack? ->
                    val clicked = raw ?: return@Function3 Menu.ClickResult.IGNORE
                    // Called when any item in inventory is clicked

                    // Call on_select and check if the resulting item is valid
                    val processed: ItemStack =
                        onSelectItem.apply(clicked.clone())
                            ?: return@Function3 Menu.ClickResult.ERROR

                    selectedItem.item(processed)
                    menu!!.update()
                    Menu.ClickResult.SUCCESS
                },
            )
        )

        // Accept item
        itemSelectorMenu.add(
            MenuItem(
                2,
                menuManager.itemSelectorAccept!!.item(),
                Function3 { p, menu, _ ->
                    val item: ItemStack? =
                        if (selectedItem.originalSelected === noItem) {
                            if (allowNothing) null else return@Function3 Menu.ClickResult.ERROR
                        } else {
                            selectedItem.originalSelected
                        }
                    menu!!.close(p!!)
                    onConfirm.apply(p, item)
                },
            )
        )

        // Cancel item
        itemSelectorMenu.add(
            MenuItem(6, menuManager.itemSelectorCancel!!.item()) { p, menu, _ ->
                menu!!.close(p!!)
                onCancel.apply(player)
                Menu.ClickResult.SUCCESS
            }
        )

        // On natural close call cancel
        itemSelectorMenu.onNaturalClose(onCancel)

        return itemSelectorMenu
    }

    /** Creates a generic selector with left-click-only item selection. */
    @JvmStatic
    fun <T, F : Filter<T?>?> genericSelector(
        context: Context<*>,
        player: Player?,
        title: String?,
        filterTitle: String?,
        things: MutableList<T?>?,
        toItem: Function1<T?, ItemStack?>?,
        filter: F?,
        onClick: Function3<Player?, Menu?, T?, Menu.ClickResult?>,
        onCancel: Consumer1<Player?>?,
    ): Menu =
        genericSelector<T, F>(
            context,
            player,
            title,
            filterTitle,
            things,
            toItem,
            filter,
            { p, menu, t, event ->
                if (!isLeftClick(event)) Menu.ClickResult.INVALID_CLICK
                else onClick.apply(p, menu, t)
            },
            onCancel,
        )

    /** Creates a generic selector with full click-event callback access. */
    @JvmStatic
    fun <T, F : Filter<T?>?> genericSelector(
        context: Context<*>,
        player: Player?,
        title: String?,
        filterTitle: String?,
        things: MutableList<T?>?,
        toItem: Function1<T?, ItemStack?>?,
        filter: F?,
        onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>?,
        onCancel: Consumer1<Player?>?,
    ): Menu =
        GenericSelector.create(
            context,
            player,
            title ?: "",
            filterTitle ?: "",
            things,
            toItem ?: Function1 { null },
            filter,
            onClick ?: Function4 { _, _, _, _ -> Menu.ClickResult.INVALID_CLICK },
            onCancel ?: Consumer1 {},
        )

    /** Creates a head selector menu with left-click-only selection callback. */
    @JvmStatic
    fun headSelector(
        context: Context<*>,
        player: Player?,
        onClick: Function3<Player?, Menu?, HeadMaterial?, Menu.ClickResult?>,
        onCancel: Consumer1<Player?>?,
    ): Menu =
        headSelector(
            context,
            player,
            { p, menu, t, event ->
                if (!isLeftClick(event)) Menu.ClickResult.INVALID_CLICK
                else onClick.apply(p, menu, t)
            },
            onCancel,
        )

    /** Creates a head selector menu with full click-event callback access. */
    @JvmStatic
    fun headSelector(
        context: Context<*>,
        player: Player?,
        onClick: Function4<Player?, Menu?, HeadMaterial?, InventoryClickEvent?, Menu.ClickResult?>?,
        onCancel: Consumer1<Player?>?,
    ): Menu {
        val menuManager = requireNotNull(context.module!!.core!!.menuManager)
        val allHeads =
            all().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.key.toString() })

        return GenericSelector.create(
            context,
            player,
            menuManager.headSelector!!.langTitle.str("§5§l${allHeads.size}"),
            menuManager.headSelector!!.langFilterTitle.str(),
            allHeads.toMutableList(),
            { h ->
                menuManager.headSelector!!
                    .itemSelectHead
                    .alternative(
                        h!!.item(),
                        "§a§l${h.name}",
                        "§6${h.category}",
                        "§b${h.tags}",
                    )
            },
            @Suppress("UNCHECKED_CAST") (HeadFilter() as Filter<HeadMaterial?>?),
            onClick ?: Function4 { _, _, _, _ -> Menu.ClickResult.INVALID_CLICK },
            onCancel ?: Consumer1 {},
        )
    }
}
