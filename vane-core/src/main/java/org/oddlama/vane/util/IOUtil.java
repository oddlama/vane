package org.oddlama.vane.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IOUtil {
	private static String read_all(Reader rd) throws IOException {
		final var sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject read_json_from_url(String url) throws IOException, JSONException {
		try (final var rd = new BufferedReader(
				new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8))) {
			return new JSONObject(read_all(rd));
		}
	}
}
