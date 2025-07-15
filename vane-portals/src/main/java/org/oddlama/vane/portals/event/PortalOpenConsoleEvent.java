package org.oddlama.vane.portals.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.oddlama.vane.portals.portal.Portal;

public class PortalOpenConsoleEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Block console;
    private Portal portal;

    public PortalOpenConsoleEvent(final Player player, final Block console, final Portal portal) {
        this.player = player;
        this.console = console;
        this.portal = portal;
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

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
