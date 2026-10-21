package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Config-backed loot registration component.
 *
 * @param T owning module type.
 * @param context component context.
 * @param baseLootKey base key prefix for registered loot entries.
 * @param defLoot supplier for default loot definitions.
 * @param desc optional config description for the loot dictionary.
 */
class LootTables<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>?,
    /** Base key prefix for all registered loot injections. */
    private val baseLootKey: NamespacedKey,
    /** Supplier of default loot-table definitions. */
    private val defLoot: () -> LootTableList?,
    /** Optional config description for loot dictionary. */
    private val desc: String? =
        "The associated loot. This is a map of loot tables (as defined by minecraft) to additional loot. This additional loot is a list of loot definitions, which specify the amount and loot percentage for a particular item.",
) : ModuleComponent<T?>(context) {
    /** Whether configured loot should be registered. */
    @ConfigBoolean(
        def = true,
        desc =
            "Whether the loot should be registered. Set to false to quickly disable all associated loot.",
    )
    var configRegisterLoot: Boolean = false

    /** Loot configuration dictionary. */
    @ConfigDict(cls = LootTableList::class, desc = "") private var configLoot: LootTableList? = null

    @Suppress("unused")
    /** Returns default loot definitions for config generation. */
    fun configLootDef(): LootTableList? = defLoot()

    @Suppress("unused")
    /** Returns config description text for the loot dictionary. */
    fun configLootDesc(): String? = desc

    /** Re-registers loot hooks according to current config state. */
    override fun onConfigChange() {
        configLoot!!.tables().filterNotNull().forEach { table ->
            table.affectedTables.forEach { tableKey ->
                module!!.lootTable(tableKey).remove(table.key(baseLootKey))
            }
        }
        if (enabled() && configRegisterLoot) {
            configLoot!!.tables().filterNotNull().forEach { table ->
                val entries = table.entries()
                table.affectedTables.forEach { tableKey ->
                    module!!
                        .lootTable(tableKey)
                        .put(table.key(baseLootKey), entries.toMutableList())
                }
            }
        }
    }

    /** Enables this component. */
    override fun onEnable() {}

    /** Disables this component. */
    override fun onDisable() {}
}
