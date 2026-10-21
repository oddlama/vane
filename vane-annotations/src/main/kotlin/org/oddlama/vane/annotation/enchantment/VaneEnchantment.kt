package org.oddlama.vane.annotation.enchantment

/**
 * Declares metadata for a custom enchantment provided by a module.
 *
 * @property name Internal name of the enchantment.
 * @property maxLevel Maximum allowed level for this enchantment.
 * @property rarity Rarity classification used for generation/loot.
 * @property curse Whether the enchantment is a curse.
 * @property tradeable Whether it can be traded via villagers.
 * @property treasure Whether it is considered treasure-only.
 * @property generateInTreasure Whether to generate in treasure chests.
 * @property allowCustom Whether custom books/items may grant this enchantment.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneEnchantment(
    val name: String,
    val maxLevel: Int = 1,
    val rarity: Rarity = Rarity.COMMON,
    val curse: Boolean = false,
    val tradeable: Boolean = false,
    val treasure: Boolean = false,
    val generateInTreasure: Boolean = false,
    val allowCustom: Boolean = false,
)
