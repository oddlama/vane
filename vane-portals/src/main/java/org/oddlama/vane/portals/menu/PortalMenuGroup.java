package org.oddlama.vane.portals.menu;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class PortalMenuGroup extends ModuleComponent<Portals> {
	public ConsoleMenu console_menu;

	public PortalMenuGroup(Context<Portals> context) {
		super(context.namespace("menus"));

		console_menu = new ConsoleMenu(get_context());
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
