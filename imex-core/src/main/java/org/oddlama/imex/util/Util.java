package org.oddlama.imex.util;

import org.bukkit.NamespacedKey;

public class Util {
	@SuppressWarnings("deprecation")
	public static NamespacedKey namespaced_key(String namespace, String key) {
		return new NamespacedKey(namespace, key);
	}

	public static long ms_to_ticks(long ms) {
		return ms / 50;
	}

	public static long ticks_to_ms(long ticks) {
		return ticks * 50;
	}
}
