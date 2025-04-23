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

public class RegionSquaremapLayer extends ModuleComponent<Regions> {

    public static final String LAYER_ID = "vane_regions_squaremap";

    @ConfigBoolean(def = false, desc = "Hide region layer by default")
    public boolean config_layer_hide;

    @ConfigInt(def = 35, min = 0, desc = "Layer ordering priority.")
    public int config_layer_priority;

    @ConfigBoolean(def = true, desc = "If the layer be filled in.")
    public boolean config_layer_fill;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    public int config_fill_color;

    @ConfigDouble(def = 0.05, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    public double config_fill_opacity;

    @ConfigBoolean(def = true, desc = "If the area marker will have outline.")
    public boolean config_line;

    @ConfigInt(def = 2, min = 1, desc = "Area marker line weight.")
    public int config_line_weight;

    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    public int config_line_color;

    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    public double config_line_opacity;

    @LangMessage
    public TranslatedMessage lang_layer_label;

    @LangMessage
    public TranslatedMessage lang_marker_label;

    private RegionSquaremapLayerDelegate delegate = null;

    public RegionSquaremapLayer(final Context<Regions> context) {
        super(
            context.group("squaremap", "Enable squaremap integration. Regions will be shown on squaremap.")
        );
    }

    public void delayed_on_enable() {
        final var plugin = get_module().getServer().getPluginManager().getPlugin("squaremap");
        if (plugin == null) {
            return;
        }

        delegate = new RegionSquaremapLayerDelegate(this);
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