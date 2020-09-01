package org.oddlama.vane.trifles.items;

import org.oddlama.vane.core.item.ModelDataEnum;

public enum ModelData implements ModelDataEnum {
	SICKLE(0),
	FILE(1);

	private int id = 0;
	private ModelData(int id) {
		this.id = id;
	}

	@Override
	public int id() { return id; }
}
