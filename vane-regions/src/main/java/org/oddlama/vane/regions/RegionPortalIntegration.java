package org.oddlama.vane.regions;

import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.event.RegionPortalRoleSettingEnforcer;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionPortalIntegration {

    public RegionPortalIntegration(Regions context, Plugin portals_plugin) {
        new RegionPortalRoleSettingEnforcer(context);

        if (portals_plugin instanceof final org.oddlama.vane.portals.Portals portals) {
            // Register callback to portals module so portals
            // can find out if two portals are in the same region group
            portals.set_is_in_same_region_group_callback((a, b) -> {
                final var reg_a = context.region_at(a.spawn());
                final var reg_b = context.region_at(b.spawn());
                if (reg_a == null || reg_b == null) {
                    return reg_a == reg_b;
                }
                return reg_a.region_group_id().equals(reg_b.region_group_id());
            });

            portals.set_player_can_use_portals_in_region_group_of_callback((player, portal) -> {
                final var region = context.region_at(portal.spawn());
                if (region == null) {
                    // No region -> no restriction.
                    return true;
                }
                final var group = region.region_group(context.get_module());
                return group.get_role(player.getUniqueId()).get_setting(RoleSetting.PORTAL);
            });
        }
    }
}

