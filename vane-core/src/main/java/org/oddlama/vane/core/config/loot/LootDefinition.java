package org.oddlama.vane.core.config.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public class LootDefinition {

    private static record Entry(double chance, int amount_min, int amount_max, String item_definition) {
        public Object serialize() {
            final HashMap<String, Object> dict = new HashMap<>();
            dict.put("chance", chance);
            dict.put("amount_min", amount_min);
            dict.put("amount_max", amount_max);
            dict.put("item", item_definition);
            return dict;
        }

        public static Entry deserialize(Map<String, Object> map) {
            if (!(map.get("chance") instanceof Double chance)) {
                throw new IllegalArgumentException("Invalid loot table entry: chance must be a double!");
            }

            if (!(map.get("amount_min") instanceof Integer amount_min)) {
                throw new IllegalArgumentException("Invalid loot table entry: amount_min must be a int!");
            }

            if (!(map.get("amount_max") instanceof Integer amount_max)) {
                throw new IllegalArgumentException("Invalid loot table entry: amount_max must be a int!");
            }

            if (!(map.get("item") instanceof String item_definition)) {
                throw new IllegalArgumentException("Invalid loot table entry: item_definition must be a String!");
            }

            return new Entry(chance, amount_min, amount_max, item_definition);
        }
    }

    public String name;
    public List<NamespacedKey> affected_tables = new ArrayList<>();
    public List<Entry> entries = new ArrayList<>();

    public LootDefinition(final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public NamespacedKey key(final NamespacedKey base_key) {
        return StorageUtil.namespaced_key(base_key.namespace(), base_key.value() + "." + name);
    }

    public LootDefinition in(final NamespacedKey table) {
        affected_tables.add(table);
        return this;
    }

    public LootDefinition in(final LootTables table) {
        return in(table.getKey());
    }

    private LootDefinition add(Entry entry) {
        entries.add(entry);
        return this;
    }

    public LootDefinition add(double chance, int amount_min, int amount_max, String item_definition) {
        return add(new Entry(chance, amount_min, amount_max, item_definition));
    }

    public Object serialize() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("tables", affected_tables.stream().map(NamespacedKey::toString).toList());
        dict.put("items", entries.stream().map(Entry::serialize).toList());
        return dict;
    }

    @SuppressWarnings("unchecked")
    public static LootDefinition deserialize(String name, Object raw_dict) {
        if (!(raw_dict instanceof Map<?, ?> dict)) {
            throw new IllegalArgumentException(
                "Invalid loot table: Argument must be a Map<String, Object>, but is " + raw_dict.getClass() + "!"
            );
        }

        final var table_dict = (Map<String, Object>) dict;
        if (!(table_dict.get("tables") instanceof List<?> tables)) {
            throw new IllegalArgumentException(
                "Invalid loot table: Argument must be a Map<String, Object>, but is " + raw_dict.getClass() + "!"
            );
        }

        if (!(table_dict.get("items") instanceof List<?> items)) {
            throw new IllegalArgumentException(
                "Invalid loot table: Argument must be a Map<String, Object>, but is " + raw_dict.getClass() + "!"
            );
        }

        final var table = new LootDefinition(name);
        for (final var e : tables) {
            if (e instanceof String key) {
                table.in(NamespacedKey.fromString(key));
            }
        }
        for (final var e : items) {
            if (e instanceof Map<?, ?> map) {
                table.add(Entry.deserialize((Map<String, Object>) map));
            }
        }

        return table;
    }

    public List<LootTableEntry> entries() {
        return entries
            .stream()
            .map(e ->
                new LootTableEntry(
                    e.chance,
                    ItemUtil.itemstack_from_string(e.item_definition).getLeft(),
                    e.amount_min,
                    e.amount_max
                )
            )
            .toList();
    }
}
