package org.oddlama.vane.core.config

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.lang.TranslatedMessageArray
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.core.material.ExtendedMaterial.Companion.from
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.util.ItemUtil

/**
 * Configurable translated item-stack definition with localized name and lore.
 *
 * @param T the owning module type.
 * @param context component context.
 * @param configNamespace namespace used for config and lang keys.
 * @param defMaterial default material when config does not override it.
 * @param defAmount default amount when config does not override it.
 * @param desc optional config section description.
 */
class TranslatedItemStack<T : Module<T?>?>(
    context: Context<T?>,
    configNamespace: String?,
    /** Default material used by config override hook. */
    private val defMaterial: ExtendedMaterial?,
    /** Default amount used by config override hook. */
    private val defAmount: Int,
    desc: String?,
) : ModuleComponent<T?>(context.namespace(configNamespace, desc)) {
    /** Configured item amount. */
    @ConfigInt(def = 1, min = 0, desc = "The item stack amount.") var configAmount: Int = 0

    /** Configured base material for this translated item stack. */
    @ConfigExtendedMaterial(
        def = "minecraft:barrier",
        desc =
            "The item stack material. Also accepts heads from the head library or from defined custom items.",
    )
    /** Configured material for generated stacks. */
    var configMaterial: ExtendedMaterial? = null

    /** Localized item display name. */
    @JvmField @LangMessage var langName: TranslatedMessage? = null

    /** Localized item lore lines. */
    @JvmField @LangMessageArray var langLore: TranslatedMessageArray? = null

    /** Creates a translated item stack from a namespaced-key material default. */
    constructor(
        context: Context<T?>,
        configNamespace: String?,
        defMaterial: NamespacedKey,
        defAmount: Int,
        desc: String?,
    ) : this(context, configNamespace, from(defMaterial), defAmount, desc)

    /** Creates a translated item stack from a Bukkit material default. */
    constructor(
        context: Context<T?>,
        configNamespace: String?,
        defMaterial: Material,
        defAmount: Int,
        desc: String?,
    ) : this(context, configNamespace, from(defMaterial), defAmount, desc)

    /** Builds a localized item stack using configured material and amount. */
    fun item(vararg args: Any?): ItemStack =
        ItemUtil.nameItem(
            configMaterial!!.item(configAmount)!!,
            langName!!.format(*args),
            langLore!!.format(*args),
        )

    /** Builds a localized item stack and applies a lore transformation callback. */
    fun itemTransformLore(
        fLore: Consumer1<MutableList<Component?>?>,
        vararg args: Any?,
    ): ItemStack {
        val lore = langLore!!.format(*args)
        fLore.apply(lore)
        return ItemUtil.nameItem(
            configMaterial!!.item(configAmount)!!,
            langName!!.format(*args),
            lore,
        )
    }

    /** Builds a localized item stack with an explicit amount override. */
    fun itemAmount(amount: Int, vararg args: Any?): ItemStack =
        ItemUtil.nameItem(
            configMaterial!!.item(amount)!!,
            langName!!.format(*args),
            langLore!!.format(*args),
        )

    /** Applies this translation metadata to an alternative base item stack. */
    fun alternative(alternative: ItemStack, vararg args: Any?): ItemStack =
        ItemUtil.nameItem(alternative, langName!!.format(*args), langLore!!.format(*args))

    /** Returns the default material for config override hooks. */
    fun configMaterialDef(): ExtendedMaterial? = defMaterial

    /** Returns the default amount for config override hooks. */
    fun configAmountDef(): Int = defAmount

    /** Enables this component. */
    override fun onEnable() {}

    /** Disables this component. */
    override fun onDisable() {}
}
