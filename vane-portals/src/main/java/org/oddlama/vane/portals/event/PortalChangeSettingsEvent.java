package org.oddlama.vane.portals.event;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.oddlama.vane.portals.portal.Portal;

public class PortalChangeSettingsEvent extends PortalEvent {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Portal portal;
	private boolean check_only;

	public PortalChangeSettingsEvent(final Player player, final Portal portal, boolean check_only) {
		this.player = player;
		this.portal = portal;
		this.check_only = check_only;
	}

	public Player getPlayer() {
		return player;
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
}
