package org.oddlama.vane.regions.menu

import java.util.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.menu.Filter
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuItem
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.RegionGroup

/**
 * Creates a [Filter.StringFilter] that matches items whose name (obtained via [nameOf]) contains
 * the search string (case-insensitive).
 */
internal fun <T> nameFilter(nameOf: (T?) -> String?): Filter.StringFilter<T?> =
    Filter.StringFilter({ item: T?, str: String? ->
        nameOf(item)!!.lowercase(Locale.getDefault()).contains(str!!)
    })

/**
 * Returns all [RegionGroup]s that the given [player] may administrate, sorted alphabetically by
 * name, together with a [Filter.StringFilter] on their names.
 */
internal fun Regions.administrableRegionGroups(
    player: Player
): Pair<MutableList<RegionGroup?>, Filter.StringFilter<RegionGroup?>> {
    /** Filtered, sorted list of administrable region groups. */
    val allRegionGroups =
        allRegionGroups()
            .asSequence()
            .filterNotNull()
            .filter { mayAdministrate(player, it) }
            .sortedBy { it.name()?.lowercase(Locale.getDefault()) ?: "" }
            .map { it as RegionGroup? }
            .toMutableList()

    return allRegionGroups to nameFilter { it?.name() }
}

/**
 * Creates a toggle [MenuItem] at [2 * 9 + col] that flips a boolean setting. [hasOverride] —
 * whether the server forces the setting (blocks toggling and shows a hint). [getSetting] — current
 * value of the setting. [onToggle] — called when the player clicks and there is no override; should
 * persist the new value.
 */
internal fun menuItemSettingToggle(
    col: Int,
    itemToggleOn: TranslatedItemStack<*>,
    itemToggleOff: TranslatedItemStack<*>,
    hasOverride: () -> Boolean,
    getSetting: () -> Boolean,
    onToggle: () -> Unit,
): MenuItem =
    object :
        MenuItem(
            2 * 9 + col,
            null,
            Function3 { _: Player?, menu: Menu?, _: MenuItem? ->
                if (hasOverride()) return@Function3 Menu.ClickResult.ERROR
                onToggle()
                requireNotNull(menu).update()
                Menu.ClickResult.SUCCESS
            },
        ) {
        /** Rebuilds toggle item based on current value and forced-override state. */
        override fun item(item: ItemStack?) {
            val maybeAddForcedHint =
                org.oddlama.vane.core.functional.Consumer1<MutableList<Component?>?> { lore ->
                    if (hasOverride()) {
                        val loreList = requireNotNull(lore)
                        loreList.add(Component.empty())
                        loreList.add(Component.text("FORCED BY SERVER"))
                    }
                }
            if (getSetting()) {
                super.item(itemToggleOn.itemTransformLore(maybeAddForcedHint))
            } else {
                super.item(itemToggleOff.itemTransformLore(maybeAddForcedHint))
            }
        }
    }

/**
 * Returns all offline players sorted by online-first then name alphabetically, together with a
 * [Filter.StringFilter] on their names.
 */
internal fun sortedOfflinePlayers():
    Pair<MutableList<OfflinePlayer?>, Filter.StringFilter<OfflinePlayer?>> {
    val allPlayers =
        Bukkit.getOfflinePlayers()
            .asSequence()
            .filter { it.name != null }
            .sortedWith(
                compareByDescending<OfflinePlayer> { it.isOnline }
                    .thenBy { it.name!!.lowercase(Locale.getDefault()) }
            )
            .map { it as OfflinePlayer? }
            .toMutableList()

    return allPlayers to nameFilter { it?.name }
}
