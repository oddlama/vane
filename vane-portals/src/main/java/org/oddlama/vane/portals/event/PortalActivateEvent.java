package org.oddlama.vane.portals.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.portals.portal.Portal;

public class PortalActivateEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Portal portal;
    private Portal target;

    public PortalActivateEvent(@Nullable final Player player, final Portal portal, final Portal target) {
        this.player = player;
        this.portal = portal;
        this.target = target;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public Portal getPortal() {
        return portal;
    }

    public Portal getTarget() {
        return target;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
