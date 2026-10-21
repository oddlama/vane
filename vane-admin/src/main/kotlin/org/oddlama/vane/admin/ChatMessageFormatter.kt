package org.oddlama.vane.admin

import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/** Formats chat, join, and quit related messages. */
class ChatMessageFormatter(context: Context<Admin?>) :
    Listener<Admin?>(
        context.group(
            "ChatMessageFormatter",
            "Enables custom formatting of chat messages like player chats and join / quit messages.",
        )
    ) {
    @LangMessage private val langPlayerChatFormat: TranslatedMessage? = null

    @LangMessage private val langPlayerJoin: TranslatedMessage? = null

    @LangMessage private val langPlayerKick: TranslatedMessage? = null

    @LangMessage private val langPlayerQuit: TranslatedMessage? = null

    /** Renderer used for chat message formatting. */
    private val chatRenderer = ChatRenderer { _, sourceDisplayName, message, _ ->
        val who = sourceDisplayName.color(NamedTextColor.AQUA)
        langPlayerChatFormat!!.strComponent(who, message)
    }

    /** Applies the custom chat renderer to async chat events. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        event.renderer(chatRenderer)
    }

    /** Suppresses default join message and broadcasts configured join text. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)
        langPlayerJoin!!.broadcastServer(event.player.playerListName().color(NamedTextColor.GOLD))
    }

    /**
     * Clears kick leave-message because quit handling already emits custom messaging.
     *
     * See Spigot issue SPIGOT-3034 for historical context.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        event.leaveMessage(Component.empty())
    }

    /** Suppresses default quit message and broadcasts configured quit or kick text. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)
        if (event.reason == PlayerQuitEvent.QuitReason.KICKED) {
            langPlayerKick!!.broadcastServer(
                event.player.playerListName().color(NamedTextColor.GOLD)
            )
        } else {
            langPlayerQuit!!.broadcastServer(
                event.player.playerListName().color(NamedTextColor.GOLD)
            )
        }
    }
}
