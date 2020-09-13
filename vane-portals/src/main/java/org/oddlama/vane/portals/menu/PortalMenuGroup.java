package org.oddlama.vane.portals.menu;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;

public class PortalMenuGroup extends ModuleComponent<Portals> {
	public ConsoleMenu console_menu;

	public PortalMenuGroup(Context<Portals> context) {
		super(context.namespace("menus"));

		console_menu = new ConsoleMenu(get_context());
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
