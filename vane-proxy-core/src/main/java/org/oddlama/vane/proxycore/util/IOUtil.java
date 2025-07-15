package org.oddlama.vane.proxycore.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

public class IOUtil {

    private static String read_all(Reader rd) throws IOException {
        final var sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject read_json_from_url(String url) throws IOException, JSONException, URISyntaxException {
        try (
            final var rd = new BufferedReader(
                new InputStreamReader(new URI(url).toURL().openStream(), StandardCharsets.UTF_8)
            )
        ) {
            return new JSONObject(read_all(rd));
        }
    }
}
