package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.module.Context

/**
 * Filter contract for selector menus.
 *
 * @param T element type being filtered.
 */
interface Filter<T> {
    /** Opens UI that lets the player configure filter state. */
    fun openFilterSettings(
        context: Context<*>,
        player: Player,
        filterTitle: String,
        returnTo: Menu?,
    )

    /** Resets filter state. */
    fun reset()

    /** Returns a filtered copy of the provided elements. */
    fun filter(things: MutableList<T?>): MutableList<T?>

    /**
     * String-backed filter implementation using an anvil input prompt.
     *
     * @param T element type being filtered.
     * @param doFilter predicate evaluating whether an element matches the filter string.
     * @param ignoreCase whether filter input should be normalized to lowercase.
     */
    class StringFilter<T>
    @JvmOverloads
    constructor(
        private val doFilter: Function2<T?, String?, Boolean?>,
        private val ignoreCase: Boolean = true,
    ) : Filter<T> {
        /** Current filter string. */
        private var str: String? = null

        /** Opens an anvil input menu to update the filter string. */
        override fun openFilterSettings(
            context: Context<*>,
            player: Player,
            filterTitle: String,
            returnTo: Menu?,
        ) {
            MenuFactory.anvilStringInput(
                    context,
                    player,
                    filterTitle,
                    ItemStack(Material.PAPER),
                    "?",
                ) { t1, t2, t3 ->
                    t2!!.close(t1!!)
                    str = t3
                    returnTo?.open(t1)
                    Menu.ClickResult.SUCCESS
                }
                .open(player)
        }

        /** Clears the active filter string. */
        override fun reset() {
            str = null
        }

        /** Filters elements using the configured predicate. */
        override fun filter(things: MutableList<T?>): MutableList<T?> {
            val fStr = str?.let { if (ignoreCase) it.lowercase() else it } ?: return things
            return things.filterTo(mutableListOf()) { doFilter.apply(it, fStr) == true }
        }
    }
}
