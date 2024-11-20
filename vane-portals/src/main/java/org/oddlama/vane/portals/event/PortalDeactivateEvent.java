package org.oddlama.vane.portals.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.portals.portal.Portal;

public class PortalDeactivateEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Portal portal;

    public PortalDeactivateEvent(final Player player, final Portal portal) {
        this.player = player;
        this.portal = portal;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public Portal getPortal() {
        return portal;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
