package org.oddlama.vane.admin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.core.Listener;

public class AutostopListener extends Listener<Admin> {

    AutostopGroup autostop;

    public AutostopListener(AutostopGroup context) {
        super(context);
        this.autostop = context;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_join(PlayerJoinEvent event) {
        autostop.abort();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_kick(PlayerKickEvent event) {
        player_leave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_quit(PlayerQuitEvent event) {
        player_leave(event.getPlayer());
    }

    private void player_leave(final Player player) {
        var players = get_module().getServer().getOnlinePlayers();
        if (players.isEmpty() || (players.size() == 1 && players.iterator().next() == player)) {
            autostop.schedule();
        }
    }
}
