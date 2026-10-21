package org.oddlama.vane.core.menu

import kotlin.math.abs
import kotlin.math.max
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function4
import org.oddlama.vane.core.menu.Menu.Companion.isLeftClick
import org.oddlama.vane.core.menu.Menu.Companion.isLeftOrRightClick
import org.oddlama.vane.core.module.Context

/**
 * State and widget helpers for paged generic selector menus.
 *
 * @param T selectable element type.
 * @param F filter type used for narrowing entries.
 * @param menuManager menu manager providing shared selector resources.
 * @param toItem renderer from element to item stack.
 * @param onClick selection click callback.
 * @param things source elements.
 * @param filter active filter implementation.
 * @param pageSize number of elements shown per page.
 */
class GenericSelector<T, F : Filter<T?>?>
private constructor(
    /** Shared menu manager resources. */
    val menuManager: MenuManager,
    /** Converts entries to display item stacks. */
    val toItem: Function1<T?, ItemStack?>,
    /** Handles selection clicks for entries. */
    val onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>,
    /** Source entries before filtering. */
    val things: MutableList<T?>,
    /** Active selector filter. */
    val filter: F,
    /** Number of entries shown per page. */
    val pageSize: Int,
) {
    /** Whether filter output should be recomputed on next update. */
    var updateFilter = true

    /** Current page index. */
    var page = 0

    /** Last valid page index. */
    var lastPage = 0

    /** Filtered elements currently shown by the selector. */
    var filteredThings: MutableList<T?> = mutableListOf()

    /** Widget rendering and handling page navigation controls. */
    class PageSelector<T, F : Filter<T?>?>(
        private val gs: GenericSelector<T, F>,
        private val slotFrom: Int,
        private val slotTo: Int,
    ) : MenuWidget {

        init {
            require(slotTo - slotFrom >= 3) { "PageSelector needs at least 3 assigned slots!" }
            require(((slotTo - slotFrom) % 2) != 0) {
                "PageSelector needs an uneven number of assigned slots!"
            }
        }

        /** Updates page control items for the current page window. */
        override fun update(menu: Menu?): Boolean {
            if (menu == null) return false
            for (slot in slotFrom until slotTo) {
                val i = slot - slotFrom
                val offset = buttonOffset(i)
                val page = pageForOffset(offset)
                val noOp = page == gs.page
                val actualOffset = page - gs.page
                val item: ItemStack? =
                    when {
                        i == (slotTo - slotFrom) / 2 ->
                            gs.menuManager.genericSelectorCurrentPage!!.item(
                                "§6${page + 1}",
                                "§6${gs.lastPage + 1}",
                                "§6${gs.filteredThings.size}",
                            )

                        noOp -> null
                        else ->
                            gs.menuManager.genericSelectorPage!!.itemAmount(
                                abs(actualOffset),
                                "§6${page + 1}",
                            )
                    }
                menu.inventory()!!.setItem(slot, item)
            }
            return true
        }

        /** Returns the logical page offset represented by a control index. */
        private fun buttonOffset(i: Int): Int =
            when {
                i <= 0 -> -BIG_JUMP_SIZE
                i >= (slotTo - slotFrom) - 1 -> BIG_JUMP_SIZE
                else -> i - (slotTo - slotFrom) / 2
            }

        /** Resolves a bounded page index for an offset. */
        private fun pageForOffset(offset: Int): Int = (gs.page + offset).coerceIn(0, gs.lastPage)

        /** Handles clicks on page controls. */
        override fun click(
            player: Player?,
            menu: Menu?,
            item: ItemStack?,
            slot: Int,
            event: InventoryClickEvent?,
        ): Menu.ClickResult {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot !in slotFrom until slotTo) return Menu.ClickResult.IGNORE
            if (menu.inventory()!!.getItem(slot) == null) return Menu.ClickResult.IGNORE
            if (!isLeftClick(event)) return Menu.ClickResult.INVALID_CLICK
            gs.page = pageForOffset(buttonOffset(slot - slotFrom))
            menu.update()
            return Menu.ClickResult.SUCCESS
        }

        /** Page-selector constants. */
        companion object {
            /** Large step size used by edge page buttons. */
            private const val BIG_JUMP_SIZE = 5
        }
    }

    /** Widget rendering selectable elements for the current page. */
    class SelectionArea<T, F : Filter<T?>?>(
        private val gs: GenericSelector<T, F>,
        private val firstSlot: Int,
    ) : MenuWidget {

        /** Updates displayed entries for the current page. */
        override fun update(menu: Menu?): Boolean {
            if (menu == null) return false
            for (i in 0 until gs.pageSize) {
                val idx = gs.page * gs.pageSize + i
                val item = gs.filteredThings.getOrNull(idx)?.let { gs.toItem.apply(it) }
                menu.inventory()!!.setItem(firstSlot + i, item)
            }
            return true
        }

        /** Handles clicks inside the paged selection area. */
        override fun click(
            player: Player?,
            menu: Menu?,
            item: ItemStack?,
            slot: Int,
            event: InventoryClickEvent?,
        ): Menu.ClickResult? {
            if (menu == null) return Menu.ClickResult.IGNORE
            if (slot < firstSlot || slot >= firstSlot + gs.pageSize) return Menu.ClickResult.IGNORE
            if (menu.inventory()!!.getItem(slot) == null) return Menu.ClickResult.IGNORE
            val idx = gs.page * gs.pageSize + (slot - firstSlot)
            return gs.onClick.apply(player, menu, gs.filteredThings[idx], event)
        }
    }

    /** Factory entry point for constructing generic selector menus. */
    companion object {
        /** Creates a generic selector menu with paging and filtering support. */
        fun <T, F : Filter<T?>?> create(
            context: Context<*>,
            player: Player?,
            title: String,
            filterTitle: String,
            things: MutableList<T?>?,
            toItem: Function1<T?, ItemStack?>,
            filter: F?,
            onClick: Function4<Player?, Menu?, T?, InventoryClickEvent?, Menu.ClickResult?>,
            onCancel: Consumer1<Player?>,
        ): Menu {
            val columns = 9
            val menuManager = requireNotNull(context.module!!.core!!.menuManager)

            @Suppress("UNCHECKED_CAST")
            val gs =
                GenericSelector(
                    menuManager = menuManager,
                    toItem = toItem,
                    onClick = onClick,
                    things = things ?: mutableListOf(),
                    filter = filter as F,
                    pageSize = 5 * columns,
                )

            val genericSelectorMenu: Menu =
                object :
                    Menu(
                        context,
                        Bukkit.createInventory(
                            null,
                            6 * columns,
                            LegacyComponentSerializer.legacySection().deserialize(title),
                        ),
                    ) {
                    override fun update(forceUpdate: Boolean) {
                        if (gs.updateFilter) {
                            gs.filteredThings = gs.filter!!.filter(gs.things)
                            gs.page = 0
                            gs.lastPage = max(0, gs.filteredThings.size - 1) / gs.pageSize
                            gs.updateFilter = false
                        }
                        super.update(forceUpdate)
                    }
                }

            genericSelectorMenu.add(SelectionArea(gs, 0))
            genericSelectorMenu.add(PageSelector(gs, gs.pageSize + 1, gs.pageSize + 8))

            // Filter item
            genericSelectorMenu.add(
                MenuItem(
                    gs.pageSize,
                    menuManager.genericSelectorFilter!!.item(),
                    Function4 { p, menu, _, event ->
                        if (!isLeftOrRightClick(event))
                            return@Function4 Menu.ClickResult.INVALID_CLICK
                        if (event!!.click == ClickType.RIGHT) {
                            gs.filter!!.reset()
                            gs.updateFilter = true
                            menu!!.update()
                        } else {
                            menu!!.close(p!!)
                            gs.filter!!.openFilterSettings(context, p, filterTitle, menu)
                            gs.updateFilter = true
                        }
                        Menu.ClickResult.SUCCESS
                    },
                )
            )

            // Cancel item
            genericSelectorMenu.add(
                MenuItem(
                    gs.pageSize + 8,
                    menuManager.genericSelectorCancel!!.item(),
                ) { p, menu, _ ->
                    menu!!.close(p!!)
                    onCancel.apply(player)
                    Menu.ClickResult.SUCCESS
                }
            )

            genericSelectorMenu.onNaturalClose(onCancel)

            return genericSelectorMenu
        }
    }
}
