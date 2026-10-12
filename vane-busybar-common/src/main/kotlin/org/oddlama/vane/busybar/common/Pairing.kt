package org.oddlama.vane.busybar.common

import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

/** Bridge tokens and the pairing strings that hand them to players. */
object Pairing {
    /** Random bytes per token; 24 bytes encode to 32 URL-safe characters. */
    private const val TOKEN_BYTES = 24

    private val random = SecureRandom()

    /** A new random bridge token. Only its [hash] should be stored. */
    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Returns the lowercase hex SHA-256 of [token], the form tokens are stored in. */
    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    /**
     * Everything a bridge needs in one line: [token], the stream [url] and, in auto TLS mode, the key
     * fingerprint [pin], for example `<token>@203.0.113.7:9123#sha256=<base64url>`. Bridges assume
     * https, so only plain HTTP keeps its prefix, which a bridge must never fall back to on its own.
     */
    fun pairingString(token: String, url: String, pin: String?): String {
        val line = if (url.startsWith("https://")) "$token@${url.removePrefix("https://")}"
        else url.replaceFirst("://", "://$token@")
        return if (pin == null) line else "$line#sha256=$pin"
    }

    /** A host with an explicit port: `host:1234` or `[2001:db8::1]:1234`. */
    private val HOST_WITH_PORT = Regex("""^(\[[^]]+]|[^:\[\]/]+):\d+$""")

    /**
     * The stream URL for bridges, or null when unknown or invalid.
     *
     * @param configured `PublicUrl`: a full http(s) URL is used as is; a bare host, optionally with
     *   a port or path, gets [scheme] and, without a port, [port].
     * @param fallbackIp the address to use with [port] when [configured] is empty, such as
     *   `server-ip` from server.properties; blank or `0.0.0.0` counts as unknown.
     * @param direct whether bridges connect to the stream's port itself rather than to a reverse
     *   proxy, so that a full URL without a port also gets [port].
     */
    fun resolvePublicUrl(configured: String?, fallbackIp: String, port: Int, scheme: String, direct: Boolean = true): String? {
        val value = configured?.trim()?.trimEnd('/').orEmpty()
        val url = when {
            "://" in value -> if (direct) withPort(value, port) else value
            value.isNotEmpty() -> {
                val host = value.substringBefore('/')
                val path = value.substring(host.length)
                val authority = when {
                    HOST_WITH_PORT.matches(host) -> host
                    ':' in host -> "[$host]:$port" // bare IPv6
                    else -> "$host:$port"
                }
                "$scheme://$authority$path"
            }
            fallbackIp.isNotBlank() && fallbackIp != "0.0.0.0" ->
                if (':' in fallbackIp) "$scheme://[$fallbackIp]:$port" else "$scheme://$fallbackIp:$port"
            else -> return null
        }
        return url.takeIf(::isStreamUrl)
    }

    /** [url] with [port] when it names none; unparsable URLs are returned unchanged for [isStreamUrl] to reject. */
    private fun withPort(url: String, port: Int): String {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return url
        }
        if (uri.port != -1 || uri.host == null) return url
        return URI(uri.scheme, uri.rawUserInfo, uri.host, port, uri.rawPath, uri.rawQuery, uri.rawFragment).toString()
    }

    /** Whether [url] is an http(s) URL with a host and nothing a pairing string adds itself. */
    private fun isStreamUrl(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return false
        }
        return uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null
    }
}
