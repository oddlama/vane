package org.oddlama.vane.portals.menu;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;

public class PortalMenuGroup extends ModuleComponent<Portals> {

    public EnterNameMenu enter_name_menu;
    public ConsoleMenu console_menu;
    public SettingsMenu settings_menu;
    public StyleMenu style_menu;

    public PortalMenuGroup(Context<Portals> context) {
        super(context.namespace("menus"));
        enter_name_menu = new EnterNameMenu(get_context());
        console_menu = new ConsoleMenu(get_context());
        settings_menu = new SettingsMenu(get_context());
        style_menu = new StyleMenu(get_context());
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
