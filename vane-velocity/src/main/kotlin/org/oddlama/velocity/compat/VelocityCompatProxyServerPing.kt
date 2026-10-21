package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.util.Favicon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.listeners.ProxyServerPing

/**
 * Mutable proxy-core ping adapter backed by a Velocity ping builder.
 *
 * @param ping source ping snapshot to mutate.
 */
class VelocityCompatProxyServerPing(ping: ServerPing) : ProxyServerPing {
    /** Velocity builder used to assemble the outgoing ping response. */
    val builder: ServerPing.Builder = ping.asBuilder()

    /**
     * Updates the MOTD text in the ping response.
     *
     * @param description new plain-text description.
     */
    override fun setDescription(description: String?) {
        // Normalize common encodings of the section sign used in legacy color codes
        var s = description.orEmpty()

        // Unescape Java-style unicode escapes for the section sign ("\\u00A7" -> '§')
        s = s.replace("\\u00A7", "\u00A7").replace("\\u00a7", "\u00A7")

        // Prefer MiniMessage when it looks like MiniMessage, otherwise try legacy parsing.
        val component =
            when {
                s.contains('<') && s.contains('>') -> {
                    try {
                        MiniMessage.miniMessage().deserialize(s)
                    } catch (e: Exception) {
                        LegacyComponentSerializer.legacySection().deserialize(s)
                    }
                }

                s.contains('\u00A7') -> LegacyComponentSerializer.legacySection().deserialize(s)
                s.contains('&') -> LegacyComponentSerializer.legacyAmpersand().deserialize(s)
                else -> Component.text(s)
            }

        builder.description(component)
    }

    /**
     * Updates the favicon in the ping response when provided.
     *
     * @param encodedFavicon base64-encoded favicon string.
     */
    override fun setFavicon(encodedFavicon: String?) {
        encodedFavicon?.let { builder.favicon(Favicon(it)) }
    }
}
