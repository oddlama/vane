package org.oddlama.vane.regions;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.oddlama.vane.regions.region.Region;

public class RegionDynmapLayerDelegate {

	private final RegionDynmapLayer parent;

	private DynmapCommonAPI dynmap_api = null;
	private MarkerAPI marker_api = null;
	private boolean dynmap_enabled = false;

	private MarkerSet marker_set = null;

	public RegionDynmapLayerDelegate(final RegionDynmapLayer parent) {
		this.parent = parent;
	}

	public Regions get_module() {
		return parent.get_module();
	}

	public void on_enable(final Plugin plugin) {
		try {
			DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {

				@Override
				public void apiEnabled(DynmapCommonAPI api) {
					dynmap_api = api;
					marker_api = dynmap_api.getMarkerAPI();					
				}
								
			});
			
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
		marker_set = marker_api.getMarkerSet(RegionDynmapLayer.LAYER_ID);
		if (marker_set == null) {
			marker_set =
				marker_api.createMarkerSet(RegionDynmapLayer.LAYER_ID, parent.lang_layer_label.str(), null, false);
		}

		if (marker_set == null) {
			get_module().log.severe("Failed to create dynmap region marker set!");
			return;
		}

		// Update attributes
		marker_set.setMarkerSetLabel(parent.lang_layer_label.str());
		marker_set.setLayerPriority(parent.config_layer_priority);
		marker_set.setHideByDefault(parent.config_layer_hide);

		// Initial update
		update_all_markers();
	}

	private String id_for(final UUID region_id) {
		return region_id.toString();
	}

	private String id_for(final Region region) {
		return id_for(region.id());
	}

	public void update_marker(final Region region) {
		if (!dynmap_enabled) {
			return;
		}

		// Area markers can't be updated.
		remove_marker(region.id());

		final var min = region.extent().min();
		final var max = region.extent().max();
		final var world_name = min.getWorld().getName();
		final var marker_id = id_for(region);
		final var marker_label = parent.lang_marker_label.str(region.name());

		final var xs = new double[] { min.getX(), max.getX() + 1 };
		final var zs = new double[] { min.getZ(), max.getZ() + 1 };
		final var area = marker_set.createAreaMarker(marker_id, marker_label, false, world_name, xs, zs, false);
		area.setRangeY(max.getY() + 1, min.getY());
		area.setLineStyle(parent.config_line_weight, parent.config_line_opacity, parent.config_line_color);
		area.setFillStyle(parent.config_fill_opacity, parent.config_fill_color);
	}

	public void remove_marker(final UUID region_id) {
		remove_marker(id_for(region_id));
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
		for (final var region : get_module().all_regions()) {
			id_set.add(id_for(region));
			update_marker(region);
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
