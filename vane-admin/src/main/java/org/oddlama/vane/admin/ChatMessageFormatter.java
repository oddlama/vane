package org.oddlama.vane.admin;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class ChatMessageFormatter extends Listener<Admin> {

    @LangMessage
    private TranslatedMessage lang_player_chat_format;

    @LangMessage
    private TranslatedMessage lang_player_join;

    @LangMessage
    private TranslatedMessage lang_player_kick;

    @LangMessage
    private TranslatedMessage lang_player_quit;

    final ChatRenderer chat_renderer;

    public ChatMessageFormatter(Context<Admin> context) {
        super(
            context.group(
                "chat_message_formatter",
                "Enables custom formatting of chat messages like player chats and join / quit messages."
            )
        );
        // Create custom chat renderer
        chat_renderer = new ChatRenderer() {
            public Component render(
                final Player source,
                final Component sourceDisplayName,
                final Component message,
                final Audience viewer
            ) {
                // TODO more sophisticated formatting?
                final var who = sourceDisplayName.color(NamedTextColor.AQUA);
                return lang_player_chat_format.str_component(who, message);
            }
        };
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on_player_chat(AsyncChatEvent event) {
        event.renderer(chat_renderer);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on_player_join(final PlayerJoinEvent event) {
        event.joinMessage(null);
        lang_player_join.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_player_kick(final PlayerKickEvent event) {
        // Bug in Spigot, doesn't do anything. But fixed in Paper since 1.17.
        // https://hub.spigotmc.org/jira/browse/SPIGOT-3034
        event.leaveMessage(Component.text(""));
        // message is handled in quit event
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_player_quit(final PlayerQuitEvent event) {
        event.quitMessage(null);
        if (event.getReason() == PlayerQuitEvent.QuitReason.KICKED) {
            lang_player_kick.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
        } else {
            lang_player_quit.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
        }
    }
}
