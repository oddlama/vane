package org.oddlama.vane.core.config.loot

import org.oddlama.vane.core.config.ConfigDictSerializable
import org.oddlama.vane.core.config.ConfigEntryNames

/**
 * Config-serializable list wrapper for [LootDefinition] entries.
 */
class LootTableList : ConfigDictSerializable {
    /** Stored loot definitions. */
    private var tables: MutableList<LootDefinition?> = mutableListOf()

    /** Returns the mutable loot definition list. */
    fun tables(): MutableList<LootDefinition?> = tables

    /** Serializes loot definitions to dictionary form. */
    override fun toDict(): MutableMap<String, Any> =
        tables.filterNotNull().associateTo(mutableMapOf()) { t -> NAMES.toYaml(t.name!!) to (t.serialize() as Any) }

    /** Loads loot definitions from dictionary form. */
    override fun fromDict(dict: MutableMap<String, Any>) {
        val names = NAMES.fromYaml(dict.keys, "loot tables")
        tables.clear()
        dict.values.zip(names).forEach { (raw, name) -> tables.add(LootDefinition.deserialize(name, raw)) }
    }

    /**
     * Construction and name-mapping helpers.
     */
    companion object {
        /** Default loot tables that are written under a PascalCase heading. */
        private val NAMES = ConfigEntryNames(
            mapOf(
                "generic" to "Generic",
                "terralith_generic" to "TerralithGeneric",
                "terralith_rare" to "TerralithRare",
                "ancientcity" to "AncientCity",
                "bastion" to "Bastion",
            )
        )

        @JvmStatic
                /** Creates a [LootTableList] from vararg definitions. */
        fun of(vararg defs: LootDefinition?): LootTableList =
            LootTableList().also { it.tables = defs.toMutableList() }

    }
}
