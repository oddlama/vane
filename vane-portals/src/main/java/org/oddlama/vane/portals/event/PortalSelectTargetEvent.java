package org.oddlama.vane.portals.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.portals.portal.Portal;

public class PortalSelectTargetEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Portal portal;
    private Portal target;
    private boolean check_only;

    public PortalSelectTargetEvent(final Player player, final Portal portal, final Portal target, boolean check_only) {
        this.player = player;
        this.portal = portal;
        this.target = target;
        this.check_only = check_only;
    }

    public Player getPlayer() {
        return player;
    }

    public Portal getPortal() {
        return portal;
    }

    public @Nullable Portal getTarget() {
        return target;
    }

    public boolean checkOnly() {
        return check_only;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
