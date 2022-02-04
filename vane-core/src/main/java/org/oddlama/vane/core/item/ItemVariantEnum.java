package org.oddlama.vane.core.item;

import org.bukkit.Keyed;

public interface ItemVariantEnum<T extends Enum<T> & Keyed> {
	/**
	 * The variant ordinal.
	 */
	public int ordinal();

	default int legacy_model_key() {
		return ordinal();
	}

	/**
	 * The variant name.
	 */
	public String name();

	/**
	 * The variant prefix.
	 */
	public String prefix();

	/**
	 * Return false to disable the associated item variant.
	 * Useful for evolving your plugins despite the use of magic values.
	 */
	public boolean enabled();
}
