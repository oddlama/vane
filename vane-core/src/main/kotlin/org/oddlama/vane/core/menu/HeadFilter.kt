package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.module.Context

/** Filter implementation for head selector entries. */
class HeadFilter : Filter<HeadMaterial> {
    /** Current lowercase filter string. */
    private var str: String? = null

    /** Opens an anvil input for editing the head filter. */
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
                str = t3?.lowercase()
                returnTo?.open(t1)
                Menu.ClickResult.SUCCESS
            }
            .open(player)
    }

    /** Clears the active filter. */
    override fun reset() {
        str = null
    }

    /** Returns whether a head matches by category. */
    private fun filterByCategory(material: HeadMaterial): Boolean =
        material.category?.lowercase()?.contains(str ?: return false) == true

    /** Returns whether a head matches by tags. */
    private fun filterByTags(material: HeadMaterial): Boolean {
        val filter = str ?: return false
        return material.tags.any { it?.lowercase()?.contains(filter) == true }
    }

    /** Returns whether a head matches by display name. */
    private fun filterByName(material: HeadMaterial): Boolean =
        material.name?.lowercase()?.contains(str ?: return false) == true

    /** Filters head materials by name, category, or tags. */
    override fun filter(things: MutableList<HeadMaterial?>): MutableList<HeadMaterial?> {
        if (str == null) return things
        return things.filterTo(mutableListOf()) { t ->
            t != null && (filterByCategory(t) || filterByTags(t) || filterByName(t))
        }
    }
}
