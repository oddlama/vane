package org.oddlama.vane.util;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

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

	public static <T> T[] prepend(T[] arr, T element) {
		final var n = arr.length;
		arr = Arrays.copyOf(arr, n + 1);
		for (int i = arr.length - 1; i > 0; --i) {
			arr[i] = arr[i - 1];
		}
		arr[0] = element;
		return arr;
	}

	public static <T> T[] append(T[] arr, T element) {
		final var n = arr.length;
		arr = Arrays.copyOf(arr, n + 1);
		arr[n] = element;
		return arr;
	}

	private static Map<Character, Long> time_multiplier;
	static {
		Map<Character, Long> mult = new HashMap<>();
		mult.put('s', 1000l);         //seconds
		mult.put('m', 60000l);        //minutes
		mult.put('h', 3600000l);      //hours
		mult.put('d', 86400000l);     //days
		mult.put('w', 604800000l);    //weeks
		mult.put('y', 31536000000l);  //years
		time_multiplier = mult;
	}

	public static long parse_time(String input) throws NumberFormatException {
		long ret = 0;

		for (String time : input.split("(?<=[^0-9])(?=[0-9])")) {
			String content[] = time.split("(?=[^0-9])");

			if (content.length != 2) {
				throw new NumberFormatException("missing multiplier");
			}

			Long mult = time_multiplier.get(content[1].replace("and", "").replaceAll("[,+\\.\\s]+", "").charAt(0));
			if (mult == null) {
				throw new NumberFormatException("\"" + content[1] + "\" is not a valid multiplier");
			}

			ret += Long.parseLong(content[0]) * mult;
		}

		return ret;
	}

	public static String format_time(long millis) {
		String ret = "";

		long days = millis / 86400000l;
		long hours = (millis / 3600000l) % 24;
		long minutes = (millis / 60000l) % 60;
		long seconds = (millis / 1000l) % 60;

		if (days > 0) {
			ret += Long.toString(days) + "d";
		}

		if (hours > 0) {
			ret += Long.toString(hours) + "h";
		}

		if (minutes > 0) {
			ret += Long.toString(minutes) + "m";
		}

		if (seconds > 0 || ret.length() == 0) {
			ret += Long.toString(seconds) + "s";
		}

		return ret;
	}
}
