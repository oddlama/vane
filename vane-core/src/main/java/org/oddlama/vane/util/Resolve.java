package org.oddlama.vane.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.json.JSONException;

public class Resolve {
	public static class Skin {

		public String texture;
		public String signature;
	}

	public static Skin resolve_skin(UUID id) throws IOException, JSONException, URISyntaxException {
		final var url = "https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false";

		final var json = IOUtil.read_json_from_url(url);
		final var skin = new Skin();
		final var obj = json.getJSONArray("properties").getJSONObject(0);
		skin.texture = obj.getString("value");
		skin.signature = obj.getString("signature");
		return skin;
	}

	public static UUID resolve_uuid(String name) throws IOException, JSONException, URISyntaxException {
		final var url = "https://api.mojang.com/users/profiles/minecraft/" + name;

		final var json = IOUtil.read_json_from_url(url);
		final var id_str = json.getString("id");
		final var uuid_str = id_str.replaceFirst(
				"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
				"$1-$2-$3-$4-$5");
		return UUID.fromString(uuid_str);
	}
}
