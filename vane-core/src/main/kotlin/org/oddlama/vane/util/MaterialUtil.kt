package org.oddlama.vane.util

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry

/** Utility helpers for resolving and classifying materials. */
object MaterialUtil {
    /** Resolves a material from its namespaced key. */
    @JvmStatic fun materialFrom(key: NamespacedKey): Material? = Registry.MATERIAL.get(key)

    /** Returns whether a block material is a seeded crop plant. */
    @JvmStatic
    fun isSeededPlant(type: Material): Boolean =
        when (type) {
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART -> true
            else -> false
        }

    /** Returns the seed item for a planted crop block material. */
    @JvmStatic
    fun seedFor(plantType: Material): Material? =
        when (plantType) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> null
        }

    /** Returns the farmland-like block required by a seed item. */
    @JvmStatic
    fun farmlandFor(seedType: Material): Material? =
        when (seedType) {
            Material.WHEAT_SEEDS,
            Material.CARROT,
            Material.POTATO,
            Material.BEETROOT_SEEDS -> Material.FARMLAND
            Material.NETHER_WART -> Material.SOUL_SAND
            else -> null
        }

    /** Returns whether the material is replaceable grass. */
    fun isReplaceableGrass(type: Material): Boolean =
        when (type) {
            Material.TALL_GRASS,
            Material.SHORT_GRASS -> true
            else -> false
        }

    /** Returns whether the material can be tilled by a hoe. */
    @JvmStatic
    fun isTillable(type: Material): Boolean =
        when (type) {
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.DIRT_PATH -> true
            else -> false
        }
}
