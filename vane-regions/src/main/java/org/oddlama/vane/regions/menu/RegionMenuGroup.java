package org.oddlama.vane.regions.menu;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;

public class RegionMenuGroup extends ModuleComponent<Regions> {

    public EnterRegionNameMenu enter_region_name_menu;
    public EnterRegionGroupNameMenu enter_region_group_name_menu;
    public EnterRoleNameMenu enter_role_name_menu;
    public MainMenu main_menu;
    public RegionGroupMenu region_group_menu;
    public RegionMenu region_menu;
    public RoleMenu role_menu;

    public RegionMenuGroup(Context<Regions> context) {
        super(context.namespace("menus"));
        enter_region_name_menu = new EnterRegionNameMenu(get_context());
        enter_region_group_name_menu = new EnterRegionGroupNameMenu(get_context());
        enter_role_name_menu = new EnterRoleNameMenu(get_context());
        main_menu = new MainMenu(get_context());
        region_group_menu = new RegionGroupMenu(get_context());
        region_menu = new RegionMenu(get_context());
        role_menu = new RoleMenu(get_context());
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
