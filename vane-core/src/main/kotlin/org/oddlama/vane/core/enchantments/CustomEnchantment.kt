package org.oddlama.vane.core.enchantments

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.loot.LootTables
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.Recipes
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.StorageUtil.namespacedKey
import org.oddlama.vane.util.snakeCaseToPascalCase

/**
 * Base class for vane custom enchantments.
 *
 * @param T owning module type.
 * @param context enchantment context.
 * @param defaultEnabled default enabled state of this enchantment group.
 */
open class CustomEnchantment<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>,
    defaultEnabled: Boolean = true,
) : Listener<T?>(null) {
    /** Resolved enchantment annotation metadata. */
    private val annotation: VaneEnchantment = javaClass.getAnnotation(VaneEnchantment::class.java)

    /** Internal enchantment name from annotation metadata. */
    val name: String

    /** Namespaced key used to register and resolve this enchantment. */
    private val key: NamespacedKey

    /** Recipe configuration for this enchantment. */
    var recipes: Recipes<T?>?

    /** Loot configuration for this enchantment. */
    var lootTables: LootTables<T?>?

    /** Localized enchantment display name. */
    @LangMessage var langName: TranslatedMessage? = null

    init {
        var context = context
        // Make namespace
        name = annotation.name
        context =
            context.group(
                "Enchantment${snakeCaseToPascalCase(name)}",
                "Enable enchantment $name",
                defaultEnabled,
            )
        setContext(context)

        // Create a namespaced key
        key = namespacedKey(module!!.namespace(), name)

        check(instances[javaClass] == null) {
            "Cannot create two instances of a custom enchantment!"
        }
        instances[javaClass] = this

        // Automatic recipes and loot table config and registration
        recipes = Recipes(getContext(), key, { defaultRecipes() })
        lootTables = LootTables(getContext(), key, { defaultLootTables() })
    }

    /** Returns the Bukkit enchantment instance for this custom enchantment key. */
    fun bukkit(): Enchantment? =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)[key]

    /** Returns the namespaced key of this enchantment. */
    fun key(): NamespacedKey = key

    /**
     * Returns the display format for the display name. By default, the color is dependent on the
     * rarity. COMMON: gray UNCOMMON: dark blue RARE: gold VERY_RARE: bold dark purple
     */
    open fun applyDisplayFormat(component: Component): Component =
        when (annotation.rarity) {
            Rarity.COMMON,
            Rarity.UNCOMMON -> component.color(NamedTextColor.DARK_AQUA)
            Rarity.RARE -> component.color(NamedTextColor.GOLD)
            Rarity.VERY_RARE ->
                component.color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD)
        }

    /**
     * Determines the display name of the enchantment. Usually you don't need to override this
     * method, as it already uses clientside translation keys and supports chat formatting.
     */
    fun displayName(level: Int): Component {
        var displayName =
            applyDisplayFormat(langName!!.format().decoration(TextDecoration.ITALIC, false))

        if (level != 1 || maxLevel() != 1) {
            val chatLevel =
                applyDisplayFormat(
                    Component.translatable("enchantment.level.$level")
                        .decoration(TextDecoration.ITALIC, false)
                )
            displayName = displayName.append(Component.text(" ")).append(chatLevel)
        }

        return displayName
    }

    /** Returns the minimum level of this enchantment (always 1). */
    fun minLevel(): Int = 1

    /**
     * The maximum level this enchantment can have. Always reflects the annotation value
     * [ ][VaneEnchantment.maxLevel].
     */
    fun maxLevel(): Int = annotation.maxLevel

    /**
     * Determines the minimum enchanting table level at which this enchantment can occur at the
     * given level.
     */
    fun minCost(level: Int): Int = 1 + level * 10

    /**
     * Determines the maximum enchanting table level at which this enchantment can occur at the
     * given level.
     */
    fun maxCost(level: Int): Int = minCost(level) + 5

    /** Whether this enchantment is marked as treasure-only. */
    val isTreasure: Boolean
        /**
         * Determines if this enchantment can be obtained with the enchanting table. Always reflects
         * the annotation value [VaneEnchantment.treasure].
         */
        get() = annotation.treasure

    /** Whether this enchantment can be offered in villager trades. */
    val isTradeable: Boolean
        /**
         * Determines if this enchantment is tradeable with villagers. Always reflects the
         * annotation value [VaneEnchantment.tradeable].
         */
        get() = annotation.tradeable

    /** Whether this enchantment is marked as a curse. */
    val isCurse: Boolean
        /**
         * Determines if this enchantment is a curse. Always reflects the annotation value
         * [ ][VaneEnchantment.curse].
         */
        get() = annotation.curse

    /**
     * Determines if this enchantment generates on treasure items. Always reflects the annotation
     * value [VaneEnchantment.generateInTreasure].
     */
    fun generateInTreasure(): Boolean = annotation.generateInTreasure

    /**
     * Determines the enchantment rarity. Always reflects the annotation value
     * [ ][VaneEnchantment.rarity].
     */
    fun rarity(): Rarity = annotation.rarity

    /** Returns whether custom items may receive this enchantment. */
    fun allowCustom(): Boolean = annotation.allowCustom

    /**
     * Determines if this enchantment is compatible with the given enchantment. By default, all
     * enchantments are compatible. Override this if you want to express conflicting enchantments.
     */
    fun isCompatible(other: Enchantment): Boolean = true

    /**
     * Determines if this enchantment can be applied to the given item. By default, this returns
     * true for all items. Item compatibility is now primarily managed by tags in the registry
     * system. This method can still be used for additional custom validation if needed.
     */
    fun canEnchant(itemStack: ItemStack): Boolean = true

    /** Returns default recipe definitions associated with this enchantment. */
    open fun defaultRecipes(): RecipeList? = RecipeList.of()

    /** Returns default loot definitions associated with this enchantment. */
    open fun defaultLootTables(): LootTableList? = LootTableList.of()

    /** Appends this enchantment descriptor to an item definition string. */
    @JvmOverloads
    protected fun on(itemDefinition: String?, level: Int = 1): String =
        "$itemDefinition#enchants{$key*$level}"

    /** Static instance tracking for custom enchantments. */
    companion object {
        /** Created custom enchantment instances keyed by class. */
        private val instances = mutableMapOf<Class<*>, CustomEnchantment<*>>()
    }
}
