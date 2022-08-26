package org.oddlama.vane.util;

public class Conversions {
	public static long ms_to_ticks(long ms) {
		return ms / 50;
	}

	public static long ticks_to_ms(long ticks) {
		return ticks * 50;
	}

	public static int exp_for_level(int level) {
		if (level < 17) {
			return level * level + 6 * level;
		} else if (level < 32) {
			return (int) (2.5 * level * level - 40.5 * level) + 360;
		} else {
			return (int) (4.5 * level * level - 162.5 * level) + 2220;
		}
	}
}
