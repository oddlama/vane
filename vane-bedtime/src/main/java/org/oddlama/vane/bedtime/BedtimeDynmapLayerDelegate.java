package org.oddlama.vane.bedtime;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

public class BedtimeDynmapLayerDelegate {

	private final BedtimeDynmapLayer parent;

	private DynmapAPI dynmap_api = null;
	private MarkerAPI marker_api = null;
	private boolean dynmap_enabled = false;

	private MarkerSet marker_set = null;
	private MarkerIcon marker_icon = null;

	public BedtimeDynmapLayerDelegate(final BedtimeDynmapLayer parent) {
		this.parent = parent;
	}

	public Bedtime get_module() {
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
		marker_set = marker_api.getMarkerSet(BedtimeDynmapLayer.LAYER_ID);
		if (marker_set == null) {
			marker_set =
				marker_api.createMarkerSet(BedtimeDynmapLayer.LAYER_ID, parent.lang_layer_label.str(), null, false);
		}

		if (marker_set == null) {
			get_module().log.severe("Failed to create dynmap bedtime marker set!");
			return;
		}

		// Update attributes
		marker_set.setMarkerSetLabel(parent.lang_layer_label.str());
		marker_set.setLayerPriority(parent.config_layer_priority);
		marker_set.setHideByDefault(parent.config_layer_hide);

		// Load marker
		marker_icon = marker_api.getMarkerIcon(parent.config_marker_icon);
		if (marker_icon == null) {
			get_module().log.severe("Failed to load dynmap bedtime marker icon!");
			return;
		}

		// Initial update
		update_all_markers();
	}

	private String id_for(final UUID player_id) {
		return player_id.toString();
	}

	private String id_for(final OfflinePlayer player) {
		return id_for(player.getUniqueId());
	}

	public boolean update_marker(final OfflinePlayer player) {
		if (!dynmap_enabled) {
			return false;
		}

		final var loc = player.getBedSpawnLocation();
		if (loc == null) {
			return false;
		}

		final var world_name = loc.getWorld().getName();
		final var marker_id = id_for(player);
		final var marker_label = parent.lang_marker_label.str(player.getName());

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
		return true;
	}

	public void remove_marker(final UUID player_id) {
		remove_marker(id_for(player_id));
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
		for (final var player : get_module().get_offline_players_with_valid_name()) {
			if (update_marker(player)) {
				id_set.add(id_for(player));
			}
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
