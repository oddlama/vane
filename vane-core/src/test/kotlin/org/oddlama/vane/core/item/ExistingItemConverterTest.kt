package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExistingItemConverterTest {
    /** Model data written by old releases, and the item each stack must turn into. */
    private val fixtures = mapOf(
        7758190 to "vane_trifles:wooden_sickle",
        7758191 to "vane_trifles:stone_sickle",
        7758192 to "vane_trifles:iron_sickle",
        7758193 to "vane_trifles:golden_sickle",
        7758194 to "vane_trifles:diamond_sickle",
        7758195 to "vane_trifles:netherite_sickle",
        7758254 to "vane_trifles:file",
        7758255 to "vane_trifles:file",
        7758256 to "vane_trifles:file",
        7758257 to "vane_trifles:file",
        7758258 to "vane_trifles:file",
        7758259 to "vane_trifles:file",
        7758318 to "vane_trifles:empty_xp_bottle",
        7758382 to "vane_trifles:small_xp_bottle",
        7758383 to "vane_trifles:medium_xp_bottle",
        7758384 to "vane_trifles:large_xp_bottle",
        7758446 to "vane_trifles:home_scroll",
        7758510 to "vane_trifles:unstable_scroll",
        7758574 to "vane_trifles:reinforced_elytra",
        7823726 to "vane_enchantments:ancient_tome",
        7823727 to "vane_enchantments:enchanted_ancient_tome",
        7823790 to "vane_enchantments:ancient_tome_of_knowledge",
        7823791 to "vane_enchantments:enchanted_ancient_tome_of_knowledge",
        7823854 to "vane_enchantments:ancient_tome_of_the_gods",
        7823855 to "vane_enchantments:enchanted_ancient_tome_of_the_gods",
    )

    @Test
    fun `maps every legacy model data id to its item`() {
        for ((modelData, key) in fixtures) {
            assertEquals(NamespacedKey.fromString(key), ExistingItemConverter.legacyItemKey(modelData), "$modelData")
        }
    }

    @Test
    fun `old wooden sickle maps although its id differs from the current one`() {
        // The wooden sickle is registered with 0x760004 today.
        assertEquals(7733252, 0x760004)
        assertEquals(NamespacedKey.fromString("vane_trifles:wooden_sickle"), ExistingItemConverter.legacyItemKey(7758190))
    }

    @Test
    fun `current and unknown ids are not legacy`() {
        assertNull(ExistingItemConverter.legacyItemKey(0x760004))
        assertNull(ExistingItemConverter.legacyItemKey(7758196))
        assertNull(ExistingItemConverter.legacyItemKey(0))
    }
}
