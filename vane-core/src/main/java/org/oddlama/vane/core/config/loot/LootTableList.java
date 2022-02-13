package org.oddlama.vane.core.config.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.oddlama.vane.core.config.ConfigDictSerializable;

public class LootTableList implements ConfigDictSerializable {
	private List<LootTableDefinition> tables = new ArrayList<>();

	public List<LootTableDefinition> tables() {
		return tables;
	}

	public Map<String, Object> to_dict() {
		return tables.stream().collect(Collectors.toMap(t -> t.key().toString(), LootTableDefinition::serialize));
	}

	public void from_dict(final Map<String, Object> dict) {
		tables.clear();
		for (final var e : dict.entrySet()) {
			tables.add(LootTableDefinition.from_dict(e.getKey(), e.getValue()));
		}
	}

	public static LootTableList of(LootTableDefinition... defs) {
		final var rl = new LootTableList();
		rl.tables = Arrays.asList(defs);
		return rl;
	}
}
