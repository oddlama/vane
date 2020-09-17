package org.oddlama.vane.enchantments.items;

import org.oddlama.vane.core.item.ModelDataEnum;

public enum ModelData implements ModelDataEnum {
	ANCIENT_TOME(0),
	ANCIENT_TOME_OF_KNOWLEDGE(1),
	ANCIENT_TOME_OF_THE_GODS(2),
	;

	private int id = 0;
	private ModelData(int id) {
		this.id = id;
	}

	@Override
	public int id() { return id; }
}
