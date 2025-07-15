package org.oddlama.vane.regions;

import java.util.UUID;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.Region;

public class RegionBlueMapLayer extends ModuleComponent<Regions> {

    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    public boolean config_hide_by_default;

    @ConfigBoolean(
        def = true,
        desc = "Set to false to make the area markers visible through terrain and other objects."
    )
    public boolean config_depth_test;

    @ConfigInt(def = 2, min = 1, desc = "Area marker line width.")
    public int config_line_width;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    public int config_fill_color;

    @ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    public double config_fill_opacity;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    public int config_line_color;

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    public double config_line_opacity;

    @LangMessage
    public TranslatedMessage lang_layer_label;

    @LangMessage
    public TranslatedMessage lang_marker_label;

    private RegionBlueMapLayerDelegate delegate = null;

    public RegionBlueMapLayer(final Context<Regions> context) {
        super(context.group("blue_map", "Enable BlueMap integration."));
    }

    public void delayed_on_enable() {
        final var plugin = get_module().getServer().getPluginManager().getPlugin("BlueMap");
        if (plugin == null) {
            return;
        }

        delegate = new RegionBlueMapLayerDelegate(this);
        delegate.on_enable(plugin);
    }

    @Override
    public void on_enable() {
        schedule_next_tick(this::delayed_on_enable);
    }

    @Override
    public void on_disable() {
        if (delegate != null) {
            delegate.on_disable();
            delegate = null;
        }
    }

    public void update_marker(final Region region) {
        if (delegate != null) {
            delegate.update_marker(region);
        }
    }

    public void remove_marker(final UUID region_id) {
        if (delegate != null) {
            delegate.remove_marker(region_id);
        }
    }

    public void update_all_markers() {
        if (delegate != null) {
            delegate.update_all_markers();
        }
    }
}
