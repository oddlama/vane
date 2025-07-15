package org.oddlama.vane.core.config.loot;

import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class LootTables<T extends Module<T>> extends ModuleComponent<T> {

    private final NamespacedKey base_loot_key;

    @ConfigBoolean(
        def = true,
        desc = "Whether the loot should be registered. Set to false to quickly disable all associated loot."
    )
    public boolean config_register_loot;

    @ConfigDict(cls = LootTableList.class, desc = "")
    private LootTableList config_loot;

    private Supplier<LootTableList> def_loot;
    private String desc;

    public LootTables(
        final Context<T> context,
        final NamespacedKey base_loot_key,
        final Supplier<LootTableList> def_loot
    ) {
        this(
            context,
            base_loot_key,
            def_loot,
            "The associated loot. This is a map of loot tables (as defined by minecraft) to additional loot. This additional loot is a list of loot definitions, which specify the amount and loot percentage for a particular item."
        );
    }

    public LootTables(
        final Context<T> context,
        final NamespacedKey base_loot_key,
        final Supplier<LootTableList> def_loot,
        final String desc
    ) {
        super(context);
        this.base_loot_key = base_loot_key;
        this.def_loot = def_loot;
        this.desc = desc;
    }

    public LootTableList config_loot_def() {
        return def_loot.get();
    }

    public String config_loot_desc() {
        return desc;
    }

    @Override
    public void on_config_change() {
        // Loot tables are processed in on_config_change and not in on_disable() / on_enable(),
        // as the current loot table modifications need to be removed even if we are disabled
        // afterwards.
        config_loot
            .tables()
            .forEach(table ->
                table.affected_tables.forEach(table_key ->
                    get_module().loot_table(table_key).remove(table.key(base_loot_key))
                )
            );
        if (enabled() && config_register_loot) {
            config_loot
                .tables()
                .forEach(table -> {
                    final var entries = table.entries();
                    table.affected_tables.forEach(table_key ->
                        get_module().loot_table(table_key).put(table.key(base_loot_key), entries)
                    );
                });
        }
    }

    @Override
    protected void on_enable() {}

    @Override
    protected void on_disable() {}
}
