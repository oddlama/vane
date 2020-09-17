package org.oddlama.vane.trifles.event;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;

public class PlayerTeleportScrollEvent extends PlayerTeleportEvent {
	private static final HandlerList handlers = new HandlerList();

	public PlayerTeleportScrollEvent(final Player player, final Location from, final Location to) {
		super(player, from, to, PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
