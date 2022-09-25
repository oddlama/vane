package org.oddlama.vane.bedtime;

import org.bukkit.OfflinePlayer;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

import java.util.UUID;

public class BedtimePlexMapLayer extends ModuleComponent<Bedtime> {

	@ConfigString(def = "bedtime-default.png", desc = "The Pl3xMap marker icon, must be the name of a file in plugins/Pl3xMap/web/images/icon.")
	public String config_marker_icon;

	private BedtimePlexMapLayerDelegate delegate = null;

	public BedtimePlexMapLayer(final Context<Bedtime> context) {
		super(context.group("plex_map", "Enable Pl3xMap integration. Player spawnpoints (beds) will then be shown on a separate layer."));
	}

	public void delayed_on_enable() {
		final var plugin = get_module().getServer().getPluginManager().getPlugin("Pl3xMap");
		if (plugin == null) {
			return;
		}

		delegate = new BedtimePlexMapLayerDelegate(this);
		delegate.on_enable();

		if (!delegate.is_enabled()) {
			// `on_enable` checks if the addon is present, if it isn't
			// we can just silently disable it here.
			delegate = null;
		}
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
		delegate.update_all_markers();
	}
}

