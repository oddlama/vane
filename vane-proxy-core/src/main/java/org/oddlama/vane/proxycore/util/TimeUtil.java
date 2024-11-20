package org.oddlama.vane.proxycore.util;

import java.util.HashMap;
import java.util.Map;

public class TimeUtil {

    private static Map<Character, Long> time_multiplier;

    static {
        Map<Character, Long> mult = new HashMap<>();
        mult.put('s', 1000L); // seconds
        mult.put('m', 60000L); // minutes
        mult.put('h', 3600000L); // hours
        mult.put('d', 86400000L); // days
        mult.put('w', 604800000L); // weeks
        mult.put('y', 31536000000L); // years
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

        long days = millis / 86400000L;
        long hours = (millis / 3600000L) % 24;
        long minutes = (millis / 60000L) % 60;
        long seconds = (millis / 1000L) % 60;

        if (days > 0) {
            ret += days + "d";
        }

        if (hours > 0) {
            ret += hours + "h";
        }

        if (minutes > 0) {
            ret += minutes + "m";
        }

        if (seconds > 0 || ret.length() == 0) {
            ret += seconds + "s";
        }

        return ret;
    }
}
