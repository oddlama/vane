package org.oddlama.vane.enchantments

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemType

/** Abstract class representing a custom enchantment registry. */
abstract class CustomEnchantmentRegistry {
    /** The key representing the enchantment. */
    val key: Key

    /** The description of the enchantment. */
    val description: Component

    /** The maximum level of the enchantment. */
    private val maxLevel: Int

    /** The tag representing the item types that this enchantment can be applied to. */
    private var supportedItemTags: TagKey<ItemType>? = null

    /** The list of specific items that this enchantment can be applied to. */
    private var supportedItems: List<TypedKey<ItemType>> = emptyList()

    /**
     * The tag representing exclusive enchantments that cannot be applied together with this
     * enchantment.
     */
    private var exclusiveWithTags: TagKey<Enchantment>? = null

    /** The list of specific enchantments that cannot be applied together with this enchantment. */
    private var exclusiveWith: List<TypedKey<Enchantment>> = emptyList()

    /**
     * Constructor for creating a custom enchantment registry with a name, supported item tag, and
     * maximum level.
     *
     * @param name The name of the enchantment.
     * @param supportedItemTags The tag representing the item types that this enchantment can be
     *   applied to.
     * @param maxLevel The maximum level of the enchantment.
     */
    constructor(name: String, supportedItemTags: TagKey<ItemType>, maxLevel: Int) {
        key = Key.key(NAMESPACE, name)
        description =
            Component.translatable("$NAMESPACE.Enchantment${snakeCaseToPascalCase(name)}.Name")
        this.supportedItemTags = supportedItemTags
        this.maxLevel = maxLevel
    }

    /**
     * Constructor for creating a custom enchantment registry with a name, supported items, and
     * maximum level.
     *
     * @param name The name of the enchantment.
     * @param supportedItems The list of specific items that this enchantment can be applied to.
     * @param maxLevel The maximum level of the enchantment.
     */
    constructor(name: String, supportedItems: List<TypedKey<ItemType>>, maxLevel: Int) {
        key = Key.key(NAMESPACE, name)
        description =
            Component.translatable("$NAMESPACE.Enchantment${snakeCaseToPascalCase(name)}.Name")
        this.supportedItems = supportedItems
        this.maxLevel = maxLevel
    }

    /**
     * Add exclusive enchantments to this enchantment: exclusive enchantments can't be on the same
     * tool.
     *
     * @param enchantments The list of exclusive enchantments.
     * @return The updated custom enchantment registry.
     */
    fun exclusiveWith(enchantments: List<TypedKey<Enchantment>>): CustomEnchantmentRegistry =
        apply {
            exclusiveWith = enchantments
        }

    /**
     * Add exclusive enchantment **tag** to this enchantment: exclusive enchantments can't be on the
     * same tool.
     *
     * @param enchantmentTag The exclusive enchantment tag.
     * @return The updated custom enchantment registry.
     */
    fun exclusiveWith(enchantmentTag: TagKey<Enchantment>?): CustomEnchantmentRegistry = apply {
        exclusiveWithTags = enchantmentTag
    }

    /** Get exclusive enchantments */
    private fun exclusiveWith(
        composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
    ): RegistryKeySet<Enchantment> {
        return if (exclusiveWithTags != null) {
            composeEvent.getOrCreateTag(requireNotNull(exclusiveWithTags))
        } else {
            RegistrySet.keySet(RegistryKey.ENCHANTMENT, exclusiveWith)
        }
    }

    /**
     * Register the enchantment in the registry
     *
     * @see
     *   [Paper Registry Documentation](https://docs.papermc.io/paper/dev/registries.create-new-entries)
     */
    fun register(
        composeEvent: RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>
    ) {
        composeEvent.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, key)) { builder ->
            builder
                .description(description)
                .supportedItems(
                    if (supportedItems.isNotEmpty()) {
                        RegistrySet.keySet(RegistryKey.ITEM, supportedItems)
                    } else {
                        composeEvent.getOrCreateTag(requireNotNull(supportedItemTags))
                    }
                )
                .anvilCost(1)
                .maxLevel(maxLevel)
                .weight(10)
                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
                .activeSlots(EquipmentSlotGroup.ANY)
                .exclusiveWith(exclusiveWith(composeEvent))
        }
    }

    /**
     * Create a typed key for the enchantment.
     *
     * @param name The name of the enchantment.
     * @return The typed key representing the enchantment.
     */
    fun typedKey(name: String): TypedKey<Enchantment> =
        TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(NAMESPACE, name))

    /** Companion object for the [CustomEnchantmentRegistry] class. */
    companion object {
        /** The namespace used for enchantments. */
        const val NAMESPACE: String = "vane_enchantments"

        /**
         * Convert a snake_case string to PascalCase.
         *
         * @param snake The snake_case string.
         * @return The converted PascalCase string.
         */
        private fun snakeCaseToPascalCase(snake: String): String {
            return snake
                .split('_')
                .filter { it.isNotEmpty() }
                .joinToString(separator = "") { part ->
                    part.replaceFirstChar { char ->
                        if (char.isLowerCase()) {
                            char.titlecase()
                        } else {
                            char.toString()
                        }
                    }
                }
        }
    }
}
