package org.oddlama.vane.trifles.items;

import org.oddlama.vane.core.item.ModelDataEnum;

public enum ModelData implements ModelDataEnum {
	SICKLE(0),
	FILE(1),
	EMPTY_XP_BOTTLE(2),
	XP_BOTTLE(3),
	HOME_SCROLL(4),
	UNSTABLE_SCROLL(5),
	REINFORCED_ELYTRA(6),
	;

	private int id = 0;
	private ModelData(int id) {
		this.id = id;
	}

	@Override
	public int id() { return id; }
}
