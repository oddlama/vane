package org.oddlama.vane.core.enchantments

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Normalizes lore and metadata for enchanted items.
 *
 * @param context listener context.
 */
class EnchantmentManager(context: Context<Core?>?) : Listener<Core?>(context) {
    /** Updates enchanted item metadata and lore. */
    fun updateEnchantedItem(itemStack: ItemStack, onlyIfEnchanted: Boolean): ItemStack =
        updateEnchantedItem(itemStack, HashMap(), onlyIfEnchanted)

    /** Updates enchanted item metadata and lore with optional additional enchantments. */
    @JvmOverloads
    fun updateEnchantedItem(
        itemStack: ItemStack,
        additionalEnchantments: MutableMap<Enchantment?, Int?>? = HashMap(),
        onlyIfEnchanted: Boolean = false,
    ): ItemStack {
        removeOldLore(itemStack)
        return itemStack
    }

    /** Removes previously generated enchantment lore lines. */
    private fun removeOldLore(itemStack: ItemStack) {
        val lore = itemStack.lore() ?: return
        lore.removeIf { isEnchantmentLore(it) }
        itemStack.lore(lore.ifEmpty { null })
    }

    /** Returns whether a component line is recognized as enchantment lore. */
    private fun isEnchantmentLore(component: Component?): Boolean {
        return component is TranslatableComponent &&
            component.key().startsWith("vane_enchantments.") ||
            ItemUtil.hasSentinel(component, SENTINEL)
    }

    /** Normalizes result item lore after result-producing inventory edits. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPrepareEnchantedEdit(event: PrepareResultEvent) {
        val result = event.result ?: return
        event.result = updateEnchantedItem(result.clone())
    }

    /** Normalizes item lore when enchanting table enchants are applied. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        updateEnchantedItem(event.item, HashMap(event.enchantsToAdd))
    }

    /** Builds a merchant recipe with normalized enchanted result metadata. */
    private fun processRecipe(recipe: MerchantRecipe): MerchantRecipe =
        MerchantRecipe(
                updateEnchantedItem(recipe.result.clone(), true),
                recipe.uses,
                recipe.maxUses,
                recipe.hasExperienceReward(),
                recipe.villagerExperience,
                recipe.priceMultiplier,
            )
            .also { newRecipe -> recipe.ingredients.forEach { newRecipe.addIngredient(it) } }

    /** Static constants for enchantment lore sentinel tagging. */
    companion object {
        /** Hover sentinel used to identify generated enchantment lore lines. */
        private val SENTINEL = namespacedKey("vane", "enchantment_lore")
    }
}
