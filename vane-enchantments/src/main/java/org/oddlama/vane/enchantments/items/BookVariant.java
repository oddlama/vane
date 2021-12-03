package org.oddlama.vane.enchantments.items;

import org.oddlama.vane.core.item.ItemVariantEnum;

public enum BookVariant implements ItemVariantEnum {
	BOOK,
	ENCHANTED_BOOK;

	@Override
	public String prefix() {
		return name().toLowerCase();
	}

	@Override
	public boolean enabled() {
		return true;
	}
}
