package org.oddlama.vane.core.material

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.util.MaterialUtil.materialFrom

/**
 * Material abstraction that can represent Bukkit materials, head materials, or custom items.
 *
 * @param key namespaced material key.
 */
class ExtendedMaterial private constructor(val key: NamespacedKey) {
    /** Backing Bukkit material if key resolves to a vanilla material. */
    private val material: Material? = materialFrom(key)

    /** Backing head material if key resolves to a head-library entry. */
    private val headMaterial: HeadMaterial? =
        if (material == null) HeadMaterialLibrary.from(key) else null

    /** Returns whether this instance wraps a plain Bukkit material. */
    val isSimpleMaterial: Boolean
        get() = material != null

    /** Builds an item stack for this extended material representation. */
    @JvmOverloads
    fun item(amount: Int = 1): ItemStack? {
        headMaterial?.let {
            return it.item().also { i -> i.amount = amount }
        }
        material?.let {
            return ItemStack(it, amount)
        }

        val customItem = Core.instance()?.itemRegistry()?.get(key)
        checkNotNull(customItem) {
            "ExtendedMaterial '$key' is neither a classic material, a head nor a custom item!"
        }
        return customItem.newStack()
    }

    /** Resolution helpers. */
    companion object {
        @JvmStatic
        /** Resolves an [ExtendedMaterial] from a key. */
        fun from(key: NamespacedKey): ExtendedMaterial? {
            val mat = ExtendedMaterial(key)
            return if (
                mat.material == null && mat.headMaterial == null && key.namespace() == "minecraft"
            )
                null
            else mat
        }

        @JvmStatic
        /** Resolves an [ExtendedMaterial] from a Bukkit material. */
        fun from(material: Material): ExtendedMaterial? = from(material.key)

        @JvmStatic
        /** Resolves an [ExtendedMaterial] from a custom item key. */
        fun from(customItem: CustomItem): ExtendedMaterial? = from(customItem.key())
    }
}
