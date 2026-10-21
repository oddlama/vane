package org.oddlama.vane.core.resourcepack

import java.net.URI
import java.net.URISyntaxException

/** Validation of resource pack URLs before they are sent to clients. */
object ResourcePackUrl {
    /**
     * Returns [url] as a URI if clients can download a resource pack from it, which requires an
     * absolute `http` or `https` URL with a host. Returns `null` for blank or malformed URLs.
     */
    fun parse(url: String?): URI? {
        if (url.isNullOrBlank()) return null
        val uri =
            try {
                URI(url.trim())
            } catch (_: URISyntaxException) {
                return null
            }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrEmpty()) return null
        return uri
    }
}
