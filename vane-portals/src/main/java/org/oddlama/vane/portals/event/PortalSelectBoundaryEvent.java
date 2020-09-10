package org.oddlama.vane.portals.event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import org.oddlama.vane.portals.PortalBoundary;

public class PortalSelectBoundaryEvent extends PortalEvent {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private PortalBoundary boundary;

	public PortalSelectBoundaryEvent(final Player player, final PortalBoundary boundary) {
		this.player = player;
		this.boundary = boundary;
	}

	public Player getPlayer() {
		return player;
	}

	public PortalBoundary getBoundary() {
		return boundary;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
