package org.oddlama.vane.core.item;

public interface ItemVariantEnum {
	/**
	 * The variant ordinal.
	 */
	public int ordinal();

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
