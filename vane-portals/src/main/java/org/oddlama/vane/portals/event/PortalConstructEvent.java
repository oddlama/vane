package org.oddlama.vane.portals.event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;

public class PortalConstructEvent extends PortalEvent {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private PortalBoundary boundary;
	private boolean check_only;

	public PortalConstructEvent(final Player player, final PortalBoundary boundary, boolean check_only) {
		this.player = player;
		this.boundary = boundary;
		this.check_only = check_only;
	}

	public Player getPlayer() {
		return player;
	}

	public PortalBoundary getBoundary() {
		return boundary;
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
