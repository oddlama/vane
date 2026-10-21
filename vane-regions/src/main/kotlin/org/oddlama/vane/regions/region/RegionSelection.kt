package org.oddlama.vane.regions.region

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.util.PlayerUtil

/** Mutable two-corner selection used while creating new regions. */
class RegionSelection(private val regions: Regions) {
    @JvmField
    /** Primary selected corner block. */
    var primary: Block? = null

    @JvmField
    /** Secondary selected corner block. */
    var secondary: Block? = null

    /** Returns selected corners when both points are set. */
    private fun selectedBlocksOrNull(): Pair<Block, Block>? {
        val primaryBlock = primary ?: return null
        val secondaryBlock = secondary ?: return null
        return primaryBlock to secondaryBlock
    }

    /** Returns whether current selection intersects any existing region. */
    fun intersectsExisting(): Boolean {
        val (primaryBlock, _) = selectedBlocksOrNull() ?: return false
        val extent = extent()
        for (r in regions.allRegions()) {
            val region = r ?: continue
            val rExtent = region.extent() ?: continue
            val rMin = rExtent.min() ?: continue
            if (rMin.world != primaryBlock.world) {
                continue
            }

            if (extent.intersectsExtent(rExtent)) {
                return true
            }
        }

        return false
    }

    /** Computes current selection price using configured pricing rules. */
    fun price(): Double {
        val (primaryBlock, secondaryBlock) = requireNotNull(selectedBlocksOrNull())
        val dx = 1 + abs(primaryBlock.x - secondaryBlock.x)
        val dy = 1 + abs(primaryBlock.y - secondaryBlock.y)
        val dz = 1 + abs(primaryBlock.z - secondaryBlock.z)
        val cost =
            ((regions.configCostYMultiplicator.pow(dy / 16.0) * regions.configCostXzBase) / 256.0) *
                dx *
                dz
        return if (regions.configEconomyAsCurrency) {
            var decimalPlaces = regions.configEconomyDecimalPlaces
            if (decimalPlaces == -1) {
                decimalPlaces = regions.economy?.fractionalDigits() ?: decimalPlaces
            }

            if (decimalPlaces >= 0) {
                BigDecimal(cost).setScale(decimalPlaces, RoundingMode.UP).toDouble()
            } else {
                cost
            }
        } else {
            ceil(cost)
        }
    }

    /** Returns whether the player can currently afford this selection. */
    fun canAfford(player: Player): Boolean {
        val price = price()
        if (price <= 0) {
            return true
        }

        if (regions.configEconomyAsCurrency) {
            return regions.economy?.has(player, price) ?: false
        } else {
            val map: MutableMap<ItemStack?, Int> = HashMap()
            val currency = regions.configCurrency ?: Material.DIAMOND
            map[ItemStack(currency)] = price.toInt()
            return PlayerUtil.hasItems(player, map)
        }
    }

    /** Returns whether the selection is complete, valid, non-overlapping, and affordable. */
    fun isValid(player: Player): Boolean {
        val (primaryBlock, secondaryBlock) = selectedBlocksOrNull() ?: return false

        // World match
        if (primaryBlock.world != secondaryBlock.world) {
            return false
        }

        val dx = 1 + abs(primaryBlock.x - secondaryBlock.x)
        val dy = 1 + abs(primaryBlock.y - secondaryBlock.y)
        val dz = 1 + abs(primaryBlock.z - secondaryBlock.z)

        // min <= extent <= max
        if (
            dx < regions.configMinRegionExtentX ||
                dy < regions.configMinRegionExtentY ||
                dz < regions.configMinRegionExtentZ ||
                dx > regions.configMaxRegionExtentX ||
                dy > regions.configMaxRegionExtentY ||
                dz > regions.configMaxRegionExtentZ
        ) {
            return false
        }

        // Assert that it doesn't intersect an existing region
        if (intersectsExisting()) {
            return false
        }

        // Check that the player can afford it
        return canAfford(player)
    }

    /** Builds a normalized extent from current selected corners. */
    fun extent(): RegionExtent {
        val (primaryBlock, secondaryBlock) = requireNotNull(selectedBlocksOrNull())
        return RegionExtent(primaryBlock, secondaryBlock)
    }
}
