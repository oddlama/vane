package org.oddlama.vane.busybar.common

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONException
import org.json.JSONObject

/**
 * Wire format of the link between vane-busybar backends and the proxy: one JSON object per line,
 * UTF-8, at most [MAX_FRAME_BYTES] long.
 */
object LinkProtocol {
    /** Protocol version, sent in `challenge` and `hello`. */
    const val VERSION = 1

    /** Largest accepted frame, newline excluded. */
    const val MAX_FRAME_BYTES = 64 * 1024

    /** Shortest secret either side accepts. */
    const val MIN_SECRET_LENGTH = 16

    /** Milliseconds between pings. */
    const val PING_INTERVAL_MS = 15_000L

    /** Milliseconds without any frame after which a link counts as dead. */
    const val TIMEOUT_MS = 45_000L

    /** Milliseconds the handshake may take. */
    const val HANDSHAKE_TIMEOUT_MS = 10_000L

    /** Random bytes per nonce. */
    private const val NONCE_BYTES = 32

    private val random = SecureRandom()

    /** A fresh base64url nonce. */
    fun nonce(): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(ByteArray(NONCE_BYTES).also(random::nextBytes))

    /**
     * Proof that the sender knows [secret], bound to the other side's [nonce] and the backend's
     * [server] name. [role] is `backend` or `proxy`, so neither side can replay the other's proof.
     */
    fun mac(secret: String, role: String, nonce: String, server: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val digest = hmac.doFinal("vane-busybar-link/$VERSION|$role|$nonce|$server".toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /** Compares two proofs in constant time. */
    fun macMatches(expected: String, actual: String?): Boolean =
        actual != null && MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())

    /**
     * Reads one frame, or returns null at the end of the stream.
     *
     * @throws IOException when the frame is too long or not a JSON object.
     */
    fun read(input: InputStream): JSONObject? {
        val line = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) {
                if (line.size() == 0) return null
                throw IOException("Link closed in the middle of a frame")
            }
            if (b == '\n'.code) break
            if (line.size() >= MAX_FRAME_BYTES)
                throw IOException("Frame exceeds $MAX_FRAME_BYTES bytes")
            line.write(b)
        }
        return try {
            JSONObject(line.toString(Charsets.UTF_8))
        } catch (e: JSONException) {
            throw IOException("Malformed frame: ${e.message}")
        }
    }

    /** Writes [frame] as one line and flushes. Callers serialize writes to the same stream. */
    fun write(output: OutputStream, frame: JSONObject) {
        output.write((frame.toString() + "\n").toByteArray())
        output.flush()
    }
}
