package org.oddlama.vane.core.itemv2.api;

import org.bukkit.NamespacedKey;

public interface CustomItemRegistry {
	public boolean has(int data);
	public int get(NamespacedKey resourceKey);

	public int allocateEphemeral(NamespacedKey resourceKey);
	public void allocate(NamespacedKey resourceKey, int data);

	public void reserve(NamespacedKey resourceKey, int data);
	public void reserveCount(NamespacedKey resourceKey, int first, int count);
	public void reserveRange(NamespacedKey resourceKey, int from, int to);
}
