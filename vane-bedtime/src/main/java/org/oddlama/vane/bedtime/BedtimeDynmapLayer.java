package org.oddlama.vane.bedtime;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class BedtimeDynmapLayer extends ModuleComponent<Bedtime> {

    public static final String LAYER_ID = "vane_bedtime.bedtime";

    @ConfigInt(def = 25, min = 0, desc = "Layer ordering priority.")
    public int config_layer_priority;

    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    public boolean config_layer_hide;

    @ConfigString(def = "house", desc = "The dynmap marker icon.")
    public String config_marker_icon;

    @LangMessage
    public TranslatedMessage lang_layer_label;

    @LangMessage
    public TranslatedMessage lang_marker_label;

    private BedtimeDynmapLayerDelegate delegate = null;

    public BedtimeDynmapLayer(final Context<Bedtime> context) {
        super(
            context.group(
                "dynmap",
                "Enable dynmap integration. Player spawnpoints (beds) will then be shown on a separate dynmap layer."
            )
        );
    }

    public void delayed_on_enable() {
        final var plugin = get_module().getServer().getPluginManager().getPlugin("dynmap");
        if (plugin == null) {
            return;
        }

        delegate = new BedtimeDynmapLayerDelegate(this);
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

    public void update_marker(final OfflinePlayer player) {
        if (delegate != null) {
            delegate.update_marker(player);
        }
    }

    public void remove_marker(final UUID player_id) {
        if (delegate != null) {
            delegate.remove_marker(player_id);
        }
    }

    public void remove_marker(final String marker_id) {
        if (delegate != null) {
            delegate.remove_marker(marker_id);
        }
    }

    public void update_all_markers() {
        if (delegate != null) {
            delegate.update_all_markers();
        }
    }
}
