package org.oddlama.vane.core.config.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.util.ItemUtil;

public class LootTableDefinition {
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

	public NamespacedKey key;
	public ArrayList<Entry> entries = new ArrayList<>();

	public LootTableDefinition(final NamespacedKey key) {
		this.key = key;
	}

	public NamespacedKey key() {
		return key;
	}

	private LootTableDefinition add(Entry entry) {
		entries.add(entry);
		return this;
	}

	public LootTableDefinition add(double chance, int amount_min, int amount_max, String item_definition) {
		return add(new Entry(chance, amount_min, amount_max, item_definition));
	}

	public Object serialize() {
		return entries.stream().map(Entry::serialize).toList();
	}

	@SuppressWarnings("unchecked")
	public static LootTableDefinition from_dict(String key, Object raw_list) {
		if (raw_list instanceof List<?> list) {
			final var table = new LootTableDefinition(NamespacedKey.fromString(key));
			for (final var e : list) {
				if (e instanceof Map<?,?> map) {
					table.add(Entry.deserialize((Map<String, Object>)map));
				}
			}
			return table;
		} else {
			throw new IllegalArgumentException("Invalid loot table: Argument must be a List<Object>, but is " + raw_list.getClass() + "!");
		}
	}

	public List<LootTableEntry> entries() {
		return entries.stream()
			.map(e -> new LootTableEntry(e.chance, ItemUtil.itemstack_from_string(e.item_definition), e.amount_min, e.amount_max))
			.toList();
	}
}
