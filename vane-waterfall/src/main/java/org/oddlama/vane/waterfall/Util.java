package org.oddlama.vane.waterfall;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Util {
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

	public static UUID add_uuid(UUID uuid, long i) {
		var msb = uuid.getMostSignificantBits();
		var lsb = uuid.getLeastSignificantBits();

		lsb += i;
		if (lsb < uuid.getLeastSignificantBits()) {
			++msb;
		}

		return new UUID(msb, lsb);
	}
}
