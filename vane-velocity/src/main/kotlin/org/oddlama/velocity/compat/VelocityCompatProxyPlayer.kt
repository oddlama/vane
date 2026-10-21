package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.Player
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.ProxyPlayer

/**
 * Velocity implementation of the proxy-core player abstraction.
 *
 * @property player wrapped Velocity player.
 */
class VelocityCompatProxyPlayer(private val player: Player) : ProxyPlayer {
    /**
     * Disconnects the player with an optional plain-text message.
     *
     * @param message disconnect reason shown to the player.
     */
    override fun disconnect(message: String?) {
        player.disconnect(Component.text(message.orEmpty()))
    }

    /** Name of the wrapped player. */
    override val name: String?
        get() = player.username

    /** Unique identifier of the wrapped player. */
    override val uniqueId: UUID?
        get() = player.uniqueId

    /** Current measured network latency in milliseconds. */
    override val ping: Long
        get() = player.ping

    /**
     * Sends a plain-text chat message to the player.
     *
     * @param message message to send.
     */
    override fun sendMessage(message: String?) {
        player.sendMessage(Component.text(message.orEmpty()))
    }

    /**
     * Sends a message that copies [copy] when clicked.
     *
     * @param message message text using section color codes.
     * @param copy text copied on click.
     * @param hover hover text using section color codes.
     */
    override fun sendCopyable(message: String, copy: String, hover: String) {
        val legacy = LegacyComponentSerializer.legacySection()
        player.sendMessage(
            legacy
                .deserialize(message)
                .clickEvent(ClickEvent.copyToClipboard(copy))
                .hoverEvent(HoverEvent.showText(legacy.deserialize(hover)))
        )
    }
}
