package org.oddlama.vane.portals;

import java.util.UUID;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.portal.Portal;

public class PortalDynmapLayer extends ModuleComponent<Portals> {

    public static final String LAYER_ID = "vane_portals.portals";

    @ConfigInt(def = 29, min = 0, desc = "Layer ordering priority.")
    public int config_layer_priority;

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    public boolean config_layer_hide;

    @ConfigString(def = "compass", desc = "The dynmap marker icon.")
    public String config_marker_icon;

    @LangMessage
    public TranslatedMessage lang_layer_label;

    @LangMessage
    public TranslatedMessage lang_marker_label;

    private PortalDynmapLayerDelegate delegate = null;

    public PortalDynmapLayer(final Context<Portals> context) {
        super(
            context.group(
                "dynmap",
                "Enable dynmap integration. Public portals will then be shown on a separate dynmap layer."
            )
        );
    }

    public void delayed_on_enable() {
        final var plugin = get_module().getServer().getPluginManager().getPlugin("dynmap");
        if (plugin == null) {
            return;
        }

        delegate = new PortalDynmapLayerDelegate(this);
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

    public void update_marker(final Portal portal) {
        if (delegate != null) {
            delegate.update_marker(portal);
        }
    }

    public void remove_marker(final UUID portal_id) {
        if (delegate != null) {
            delegate.remove_marker(portal_id);
        }
    }

    public void update_all_markers() {
        if (delegate != null) {
            delegate.update_all_markers();
        }
    }
}
