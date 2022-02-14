package org.oddlama.vane.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

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
		mult.put('s', 1000l); //seconds
		mult.put('m', 60000l); //minutes
		mult.put('h', 3600000l); //hours
		mult.put('d', 86400000l); //days
		mult.put('w', 604800000l); //weeks
		mult.put('y', 31536000000l); //years
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

	public static int exp_for_level(int level) {
		if (level < 17) {
			return level * level + 6 * level;
		} else if (level < 32) {
			return (int) (2.5 * level * level - 40.5 * level) + 360;
		} else {
			return (int) (4.5 * level * level - 162.5 * level) + 2220;
		}
	}

	private static String read_all(Reader rd) throws IOException {
		final var sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject read_json_from_url(String url) throws IOException, JSONException {
		try (
			final var rd = new BufferedReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8))
		) {
			return new JSONObject(read_all(rd));
		}
	}

	public static UUID resolve_uuid(String name) {
		final var url = "https://api.mojang.com/users/profiles/minecraft/" + name;
		try {
			final var json = read_json_from_url(url);
			final var id_str = json.getString("id");
			final var uuid_str = id_str.replaceFirst(
				"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
				"$1-$2-$3-$4-$5"
			);
			return UUID.fromString(uuid_str);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Failed to resolve UUID for player '" + name + "'", e);
			return null;
		}
	}

	public static class Skin {

		public String texture;
		public String signature;
	}

	public static Skin resolve_skin(UUID id) {
		final var url = "https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false";
		try {
			final var json = read_json_from_url(url);
			final var skin = new Skin();
			final var obj = json.getJSONArray("properties").getJSONObject(0);
			skin.texture = obj.getString("value");
			skin.signature = obj.getString("signature");
			return skin;
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Failed to resolve skin for uuid '" + id + "'", e);
			return null;
		}
	}

	public static NamespacedKey subkey(NamespacedKey key, String sub) {
		return namespaced_key(key.namespace(), key.value() + "." + sub);
	}

	public static boolean storage_has_location(@NotNull PersistentDataContainer data, NamespacedKey key) {
		return data.has(subkey(key, "world"), PersistentDataType.STRING);
	}

	public static Location storage_get_location(@NotNull PersistentDataContainer data, NamespacedKey key, Location def) {
		try {
			final var world_id = data.get(subkey(key, "world"), PersistentDataType.STRING);
			final var x = data.get(subkey(key, "x"), PersistentDataType.DOUBLE);
			final var y = data.get(subkey(key, "y"), PersistentDataType.DOUBLE);
			final var z = data.get(subkey(key, "z"), PersistentDataType.DOUBLE);
			final var yaw = data.get(subkey(key, "yaw"), PersistentDataType.FLOAT);
			final var pitch = data.get(subkey(key, "pitch"), PersistentDataType.FLOAT);
			final var world = Bukkit.getWorld(UUID.fromString(world_id));
			if (world == null) {
				return def;
			}
			return new Location(world, x, y, z, yaw, pitch);
		} catch (IllegalArgumentException | NullPointerException e) {
			return def;
		}
	}

	public static void storage_remove_location(@NotNull PersistentDataContainer data, NamespacedKey key) {
		data.remove(subkey(key, "world"));
		data.remove(subkey(key, "x"));
		data.remove(subkey(key, "y"));
		data.remove(subkey(key, "z"));
		data.remove(subkey(key, "yaw"));
		data.remove(subkey(key, "pitch"));
	}

	public static void storage_set_location(@NotNull PersistentDataContainer data, NamespacedKey key, @NotNull Location location) {
		data.set(subkey(key, "world"), PersistentDataType.STRING, location.getWorld().getUID().toString());
		data.set(subkey(key, "x"), PersistentDataType.DOUBLE, location.getX());
		data.set(subkey(key, "y"), PersistentDataType.DOUBLE, location.getY());
		data.set(subkey(key, "z"), PersistentDataType.DOUBLE, location.getZ());
		data.set(subkey(key, "yaw"), PersistentDataType.FLOAT, location.getYaw());
		data.set(subkey(key, "pitch"), PersistentDataType.FLOAT, location.getPitch());
	}
}
