package org.oddlama.vane.portals;

import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.portal.Portal;

import java.util.UUID;

public class PortalPlexMapLayer extends ModuleComponent<Portals> {

	@ConfigString(def = "portal-default.png", desc = "The Pl3xMap marker icon, must be the name of a file in plugins/Pl3xMap/web/images/icon.")
	public String config_marker_icon;

	private PortalPlexMapLayerDelegate delegate = null;

	public PortalPlexMapLayer(final Context<Portals> context) {
		super(context.group("plex_map", "Enable Pl3xMap integration to show public portals."));
	}

	public void delayed_on_enable() {
		final var plugin = get_module().getServer().getPluginManager().getPlugin("Pl3xMap");
		if (plugin == null) {
			return;
		}

		delegate = new PortalPlexMapLayerDelegate(this);
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
		delegate.update_all_markers();
	}
}
