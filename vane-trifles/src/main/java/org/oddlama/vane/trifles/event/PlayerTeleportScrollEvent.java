package org.oddlama.vane.trifles.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportScrollEvent extends PlayerTeleportEvent {

    private static final HandlerList handlers = new HandlerList();

    public PlayerTeleportScrollEvent(final Player player, final Location from, final Location to) {
        super(player, from, to, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
