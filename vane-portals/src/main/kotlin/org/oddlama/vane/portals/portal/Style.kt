package org.oddlama.vane.portals.portal

import java.io.IOException
import java.util.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.util.StorageUtil

/**
 * Portal style definition mapping block types to active and inactive materials.
 *
 * @property key unique style key or null for anonymous temporary styles.
 */
class Style(private var key: NamespacedKey?) {
    /** Active-state material mapping per portal block type. */
    private var activeMaterials: MutableMap<PortalBlock.Type, Material> =
        EnumMap(PortalBlock.Type::class.java)

    /** Inactive-state material mapping per portal block type. */
    private var inactiveMaterials: MutableMap<PortalBlock.Type, Material> =
        EnumMap(PortalBlock.Type::class.java)

    /** Returns this style key. */
    fun key() = key

    /** Returns the material configured for [type] in the selected [active] state. */
    fun material(active: Boolean, type: PortalBlock.Type) =
        if (active) activeMaterials[type] else inactiveMaterials[type]

    /** Sets a material mapping and rejects duplicate keys. */
    fun setMaterial(active: Boolean, type: PortalBlock.Type, material: Material) {
        setMaterial(active, type, material, false)
    }

    /** Sets a material mapping with optional overwrite behavior. */
    fun setMaterial(
        active: Boolean,
        type: PortalBlock.Type,
        material: Material,
        overwrite: Boolean,
    ) {
        val map = if (active) activeMaterials else inactiveMaterials

        if (!overwrite && map.containsKey(type)) {
            throw RuntimeException(
                "Invalid style definition! PortalBlock.Type.$type was specified multiple times."
            )
        }
        map[type] = material
    }

    /** Validates that all portal block types are mapped for active and inactive states. */
    fun checkValid() {
        // Checks if every key is set
        for (type in PortalBlock.Type.entries) {
            if (!activeMaterials.containsKey(type)) {
                throw RuntimeException(
                    "Invalid style definition! Active state for PortalBlock.Type.$type was not specified!"
                )
            }
            if (!inactiveMaterials.containsKey(type)) {
                throw RuntimeException(
                    "Invalid style definition! Inactive state for PortalBlock.Type.$type was not specified!"
                )
            }
        }
    }

    /** Creates a deep copy of this style using [newKey]. */
    fun copy(newKey: NamespacedKey?): Style {
        val copy = Style(newKey)
        copy.activeMaterials = EnumMap(activeMaterials)
        copy.inactiveMaterials = EnumMap(inactiveMaterials)
        return copy
    }

    /** Serialization helpers and default-style factory methods. */
    companion object {
        /** Serializes a style into a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val style = o as Style
            val json = JSONObject()
            json.put("key", PersistentSerializer.toJson(NamespacedKey::class.java, style.key))
            try {
                json.put(
                    "activeMaterials",
                    PersistentSerializer.toJson(
                        Style::class.java.getDeclaredField("activeMaterials"),
                        style.activeMaterials,
                    ),
                )
                json.put(
                    "inactiveMaterials",
                    PersistentSerializer.toJson(
                        Style::class.java.getDeclaredField("inactiveMaterials"),
                        style.inactiveMaterials,
                    ),
                )
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }
            return json
        }

        /** Deserializes a style from a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): Style {
            val json = o as JSONObject
            val style = Style(null)
            style.key = PersistentSerializer.fromJson(NamespacedKey::class.java, json.get("key"))
            try {
                @Suppress("UNCHECKED_CAST")
                style.activeMaterials =
                    PersistentSerializer.fromJson(
                        Style::class.java.getDeclaredField("activeMaterials"),
                        json.get("activeMaterials"),
                    ) as MutableMap<PortalBlock.Type, Material>
                @Suppress("UNCHECKED_CAST")
                style.inactiveMaterials =
                    PersistentSerializer.fromJson(
                        Style::class.java.getDeclaredField("inactiveMaterials"),
                        json.get("inactiveMaterials"),
                    ) as MutableMap<PortalBlock.Type, Material>
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }
            return style
        }

        /** Returns the shared namespaced key used for the built-in default style. */
        @JvmStatic
        fun defaultStyleKey(): NamespacedKey {
            return StorageUtil.namespacedKey("vane_portals", "portal_style_default")
        }

        /** Builds the built-in default portal style definition. */
        @JvmStatic
        fun defaultStyle(): Style {
            val style = Style(defaultStyleKey())
            style.setMaterial(true, PortalBlock.Type.BOUNDARY1, Material.OBSIDIAN)
            style.setMaterial(true, PortalBlock.Type.BOUNDARY2, Material.CRYING_OBSIDIAN)
            style.setMaterial(true, PortalBlock.Type.BOUNDARY3, Material.GOLD_BLOCK)
            style.setMaterial(true, PortalBlock.Type.BOUNDARY4, Material.GILDED_BLACKSTONE)
            style.setMaterial(true, PortalBlock.Type.BOUNDARY5, Material.EMERALD_BLOCK)
            style.setMaterial(true, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE)
            style.setMaterial(true, PortalBlock.Type.ORIGIN, Material.OBSIDIAN)
            style.setMaterial(true, PortalBlock.Type.PORTAL, Material.END_GATEWAY)
            style.setMaterial(false, PortalBlock.Type.BOUNDARY1, Material.OBSIDIAN)
            style.setMaterial(false, PortalBlock.Type.BOUNDARY2, Material.CRYING_OBSIDIAN)
            style.setMaterial(false, PortalBlock.Type.BOUNDARY3, Material.GOLD_BLOCK)
            style.setMaterial(false, PortalBlock.Type.BOUNDARY4, Material.GILDED_BLACKSTONE)
            style.setMaterial(false, PortalBlock.Type.BOUNDARY5, Material.EMERALD_BLOCK)
            style.setMaterial(false, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE)
            style.setMaterial(false, PortalBlock.Type.ORIGIN, Material.OBSIDIAN)
            style.setMaterial(false, PortalBlock.Type.PORTAL, Material.AIR)
            return style
        }
    }
}
