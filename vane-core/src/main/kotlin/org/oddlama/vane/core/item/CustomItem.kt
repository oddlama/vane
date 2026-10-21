package org.oddlama.vane.core.item

import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.loot.LootTables
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.Recipes
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.StorageUtil.namespacedKey
import org.oddlama.vane.util.snakeCaseToPascalCase

/**
 * Base implementation for vane custom items.
 *
 * @param T the owning module type.
 * @param context item context.
 * @param nameOverride optional runtime name override.
 * @param customModelDataOverride optional runtime custom model data override.
 */
open class CustomItem<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>,
    nameOverride: String? = null,
    customModelDataOverride: Int? = null,
) : Listener<T?>(null), CustomItem {
    /** Resolved `@VaneItem` annotation. */
    private var annotation: VaneItem? = null

    /** Namespaced key of this custom item. */
    var key: NamespacedKey

    /** Recipe configuration container for this item. */
    var recipes: Recipes<T?>?

    /** Loot table configuration container for this item. */
    var lootTables: LootTables<T?>?

    /** Localized item display name. */
    @LangMessage var langName: TranslatedMessage? = null

    /** Configured durability override for this item. */
    @ConfigInt(
        def = 0,
        min = 0,
        desc =
            "The durability of this item. Set to 0 to use the durability properties of whatever base material the item is made of.",
    )
    /** Effective configured durability value. */
    private var configDurability = 0

    /** Optional runtime item name override. */
    private val nameOverride: String?

    /** Optional runtime model data override. */
    private val customModelDataOverride: Int?

    init {
        var context = context
        var cls: Class<*>? = javaClass
        while (this.annotation == null && cls != null) {
            this.annotation = cls.getAnnotation(VaneItem::class.java)
            cls = cls.getSuperclass()
        }
        checkNotNull(this.annotation) { "Could not find @VaneItem annotation on $javaClass" }

        this.nameOverride = nameOverride
        this.customModelDataOverride = customModelDataOverride

        context = context.group("Item" + snakeCaseToPascalCase(name()), "Enable item " + name())
        setContext(context)

        this.key = namespacedKey(module!!.namespace(), name())
        recipes = Recipes(getContext(), this.key, { this.defaultRecipes() })
        lootTables = LootTables(getContext(), this.key, { this.defaultLootTables() })

        module!!.core?.itemRegistry()?.register(this)
    }

    /** Returns the item key. */
    override fun key(): NamespacedKey = key

    /** Returns the configured item name. */
    fun name(): String = nameOverride ?: annotation!!.name

    /** Returns whether this item is enabled. */
    override fun enabled(): Boolean {
        return annotation!!.enabled && super.enabled()
    }

    /** Returns the item definition version. */
    override fun version(): Int = annotation!!.version

    /** Returns the base material for newly created stacks. */
    override fun baseMaterial(): Material = annotation!!.base

    /** Returns the custom model data id. */
    override fun customModelData(): Int = customModelDataOverride ?: annotation!!.modelData

    /** Returns the translated display name component. */
    override fun displayName(): Component? =
        langName!!.format().decoration(TextDecoration.ITALIC, false)

    /** Returns the default durability used by config override hooks. */
    fun configDurabilityDef(): Int = annotation!!.durability

    /** Returns the active durability value. */
    override fun durability(): Int = configDurability

    /** Applies item-specific stack updates. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack = itemStack

    /** Returns behaviors that should be inhibited for this item. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE)
    }

    /** Returns default recipe definitions. */
    open fun defaultRecipes(): RecipeList? {
        return RecipeList.of()
    }

    /** Returns default loot table definitions. */
    open fun defaultLootTables(): LootTableList? {
        return LootTableList.of()
    }
}
