package org.oddlama.vane.portals.event;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.portals.portal.Portal;

public class PortalLinkConsoleEvent extends PortalEvent {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Portal portal;
    private Block console;
    private List<Block> portal_blocks;
    private boolean check_only;
    private boolean cancel_if_not_owner = true;

    public PortalLinkConsoleEvent(
        final Player player,
        final Block console,
        final List<Block> portal_blocks,
        boolean check_only,
        @Nullable final Portal portal
    ) {
        this.player = player;
        this.console = console;
        this.portal_blocks = portal_blocks;
        this.check_only = check_only;
        this.portal = portal;
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

    public List<Block> getPortalBlocks() {
        return portal_blocks;
    }

    public boolean checkOnly() {
        return check_only;
    }

    public @Nullable Portal getPortal() {
        return portal;
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
        if (cancel_if_not_owner && portal != null) {
            cancelled |= !player.getUniqueId().equals(portal.owner());
        }
        return cancelled;
    }
}
