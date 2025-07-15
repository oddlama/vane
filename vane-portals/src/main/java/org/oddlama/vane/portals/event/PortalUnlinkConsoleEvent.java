package org.oddlama.vane.portals.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.oddlama.vane.portals.portal.Portal;

public class PortalUnlinkConsoleEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Block console;
    private Portal portal;
    private boolean check_only;
    private boolean cancel_if_not_owner = true;

    public PortalUnlinkConsoleEvent(final Player player, final Block console, final Portal portal, boolean check_only) {
        this.player = player;
        this.console = console;
        this.portal = portal;
        this.check_only = check_only;
    }

    public void setCancelIfNotOwner(boolean cancel_if_not_owner) {
        this.cancel_if_not_owner = cancel_if_not_owner;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getConsole() {
        return console;
    }

    public Portal getPortal() {
        return portal;
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

    @Override
    public boolean isCancelled() {
        var cancelled = super.isCancelled();
        if (cancel_if_not_owner) {
            cancelled |= !player.getUniqueId().equals(portal.owner());
        }
        return cancelled;
    }
}
