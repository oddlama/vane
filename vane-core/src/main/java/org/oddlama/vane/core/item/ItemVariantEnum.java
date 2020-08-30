package org.oddlama.vane.core.item;

public interface ItemVariantEnum {
	/**
	 * The variant id.
	 */
	public int id();

	/**
	 * The variant identifier.
	 */
	public String identifier();

	/**
	 * Return false to disable the associated item variant.
	 * Useful for evolving your plugins despite the use of magic values.
	 */
	public boolean enabled();
}
