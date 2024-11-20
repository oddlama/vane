package org.oddlama.vane.regions;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionGlobalRoleOverrides extends ModuleComponent<Regions> {

    @ConfigInt(
        def = 0,
        min = -1,
        max = 1,
        desc = "Overrides the admin permission. Be careful, this is almost never what you want and may result in immutable regions."
    )
    public int config_admin;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the build permission.")
    public int config_build;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the use permission.")
    public int config_use;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the container permission.")
    public int config_container;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides the portal permission.")
    public int config_portal;

    public RegionGlobalRoleOverrides(Context<Regions> context) {
        super(
            context.namespace(
                "global_role_overrides",
                "This controls global role setting overrides for all roles in every region on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all roles in every region, `-1` force-disables respectively. Force-disable naturally also affects the owner, so be careful!"
            )
        );
    }

    public int get_override(final RoleSetting setting) {
        switch (setting) {
            case ADMIN:
                return config_admin;
            case BUILD:
                return config_build;
            case USE:
                return config_use;
            case CONTAINER:
                return config_container;
            case PORTAL:
                return config_portal;
        }
        return 0;
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
