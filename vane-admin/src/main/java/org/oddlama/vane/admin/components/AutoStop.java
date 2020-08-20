package org.oddlama.vane.admin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class AutoStopComponent extends ModuleGroup<Admin> {
	public AutoStopListener listener = new AutoStopListener(this);

	public AutoStopComponent(Context context) {
		super(context, "autostop", "Enable automatic server stop after certain time without online players.");
	}
}
