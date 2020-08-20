package org.oddlama.vane.admin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class AutostopListener extends Listener<Admin> {
	public AutostopListener(Context<Admin> context) {
		super(context);
	}
}
