package org.oddlama.vane.portals;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.oddlama.vane.portals.portal.Portal;

public class PortalDynmapLayerDelegate {

	private final PortalDynmapLayer parent;

	private DynmapAPI dynmap_api = null;
	private MarkerAPI marker_api = null;
	private boolean dynmap_enabled = false;

	private MarkerSet marker_set = null;
	private MarkerIcon marker_icon = null;

	public PortalDynmapLayerDelegate(final PortalDynmapLayer parent) {
		this.parent = parent;
	}

	public Portals get_module() {
		return parent.get_module();
	}

	public void on_enable(final Plugin plugin) {
		try {
			dynmap_api = (DynmapAPI) plugin;
			marker_api = dynmap_api.getMarkerAPI();
		} catch (Exception e) {
			get_module().log.log(Level.WARNING, "Error while enabling dynmap integration!", e);
			return;
		}

		if (marker_api == null) {
			return;
		}

		get_module().log.info("Enabling dynmap integration");
		dynmap_enabled = true;
		create_or_load_layer();
	}

	public void on_disable() {
		if (!dynmap_enabled) {
			return;
		}

		get_module().log.info("Disabling dynmap integration");
		dynmap_enabled = false;
		dynmap_api = null;
		marker_api = null;
	}

	private void create_or_load_layer() {
		// Create or retrieve layer
		marker_set = marker_api.getMarkerSet(PortalDynmapLayer.LAYER_ID);
		if (marker_set == null) {
			marker_set =
				marker_api.createMarkerSet(PortalDynmapLayer.LAYER_ID, parent.lang_layer_label.str(), null, false);
		}

		if (marker_set == null) {
			get_module().log.severe("Failed to create dynmap portal marker set!");
			return;
		}

		// Update attributes
		marker_set.setMarkerSetLabel(parent.lang_layer_label.str());
		marker_set.setLayerPriority(parent.config_layer_priority);
		marker_set.setHideByDefault(parent.config_layer_hide);

		// Load marker
		marker_icon = marker_api.getMarkerIcon(parent.config_marker_icon);
		if (marker_icon == null) {
			get_module().log.severe("Failed to load dynmap portal marker icon!");
			return;
		}

		// Initial update
		update_all_markers();
	}

	private String id_for(final UUID portal_id) {
		return portal_id.toString();
	}

	private String id_for(final Portal portal) {
		return id_for(portal.id());
	}

	public void update_marker(final Portal portal) {
		if (!dynmap_enabled) {
			return;
		}

		// Don't show private portals
		if (portal.visibility() == Portal.Visibility.PRIVATE) {
			remove_marker(portal.id());
			return;
		}

		final var loc = portal.spawn();
		final var world_name = loc.getWorld().getName();
		final var marker_id = id_for(portal);
		final var marker_label = parent.lang_marker_label.str(portal.name());

		marker_set.createMarker(
			marker_id,
			marker_label,
			world_name,
			loc.getX(),
			loc.getY(),
			loc.getZ(),
			marker_icon,
			false
		);
	}

	public void remove_marker(final UUID portal_id) {
		remove_marker(id_for(portal_id));
	}

	public void remove_marker(final String marker_id) {
		if (!dynmap_enabled || marker_id == null) {
			return;
		}

		remove_marker(marker_set.findMarker(marker_id));
	}

	public void remove_marker(final Marker marker) {
		if (!dynmap_enabled || marker == null) {
			return;
		}

		marker.deleteMarker();
	}

	public void update_all_markers() {
		if (!dynmap_enabled) {
			return;
		}

		// Update all existing
		final var id_set = new HashSet<String>();
		for (final var portal : get_module().all_available_portals()) {
			id_set.add(id_for(portal));
			update_marker(portal);
		}

		// Remove orphaned
		for (final var marker : marker_set.getMarkers()) {
			final var id = marker.getMarkerID();
			if (id != null && !id_set.contains(id)) {
				remove_marker(marker);
			}
		}
	}
}
