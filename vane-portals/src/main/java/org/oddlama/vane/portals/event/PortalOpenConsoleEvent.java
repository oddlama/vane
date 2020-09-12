package org.oddlama.vane.portals.event;

import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;
import org.bukkit.event.HandlerList;

import org.bukkit.block.Block;

import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;

public class PortalOpenConsoleEvent extends PortalEvent {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Block console;
	private UUID portal_id;

	public PortalOpenConsoleEvent(final Player player, final Block console, final UUID portal_id) {
		this.player = player;
		this.console = console;
		this.portal_id = portal_id;
	}

	public Player getPlayer() {
		return player;
	}

	public Block getConsole() {
		return console;
	}

	public UUID getPortalId() {
		return portal_id;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
