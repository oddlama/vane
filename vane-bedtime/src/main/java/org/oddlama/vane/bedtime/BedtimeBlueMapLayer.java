package org.oddlama.vane.bedtime;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class BedtimeBlueMapLayer extends ModuleComponent<Bedtime> {

    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    public boolean config_hide_by_default;

    @LangMessage
    public TranslatedMessage lang_layer_label;

    @LangMessage
    public TranslatedMessage lang_marker_label;

    private BedtimeBlueMapLayerDelegate delegate = null;

    public BedtimeBlueMapLayer(final Context<Bedtime> context) {
        super(context.group("blue_map", "Enable BlueMap integration to show player spawnpoints (beds)."));
    }

    public void delayed_on_enable() {
        final var plugin = get_module().getServer().getPluginManager().getPlugin("BlueMap");
        if (plugin == null) {
            return;
        }

        delegate = new BedtimeBlueMapLayerDelegate(this);
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

    public void update_all_markers() {
        if (delegate != null) {
            delegate.update_all_markers();
        }
    }
}
