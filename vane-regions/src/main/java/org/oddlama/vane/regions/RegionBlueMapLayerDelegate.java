package org.oddlama.vane.regions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.region.Region;

public class RegionBlueMapLayerDelegate {

	public static final String MARKER_SET_ID = "vane_regions.regions";

	private final RegionBlueMapLayer parent;

	private boolean bluemap_enabled = false;

	public RegionBlueMapLayerDelegate(final RegionBlueMapLayer parent) {
		this.parent = parent;
	}

	public Regions get_module() {
		return parent.get_module();
	}

	public void on_enable(final Plugin plugin) {
		BlueMapAPI.onEnable(api -> {
			get_module().log.info("Enabling BlueMap integration");
			bluemap_enabled = true;

			// Create marker sets
			for (final var world : get_module().getServer().getWorlds()) {
				create_marker_set(api, world);
			}

			update_all_markers();
		});
	}

	public void on_disable() {
		if (!bluemap_enabled) {
			return;
		}

		get_module().log.info("Disabling BlueMap integration");
		bluemap_enabled = false;
	}

	// world_id -> MarkerSet
	private final HashMap<UUID, MarkerSet> marker_sets = new HashMap<>();
	private void create_marker_set(final BlueMapAPI api, final World world) {
		if (marker_sets.containsKey(world.getUID())) {
			return;
		}

		final var marker_set = MarkerSet.builder()
			.label(parent.lang_layer_label.str())
			.toggleable(true)
			.defaultHidden(parent.config_hide_by_default)
			.build();

		api.getWorld(world).ifPresent(bm_world -> {
			for (final var map : bm_world.getMaps()) {
				map.getMarkerSets().put(MARKER_SET_ID, marker_set);
			}
		});

		marker_sets.put(world.getUID(), marker_set);
	}

	public void update_marker(final Region region) {
		remove_marker(region.id());
		final var min = region.extent().min();
		final var max = region.extent().max();
		final var shape = Shape.createRect(min.getX(), min.getZ(), max.getX() + 1, max.getZ() + 1);

		final var marker = ExtrudeMarker.builder()
			.shape(shape, min.getY(), max.getY() + 1)
			.label(parent.lang_marker_label.str(region.name()))
			.lineWidth(parent.config_line_width)
			.lineColor(new Color(parent.config_line_color, (float)parent.config_line_opacity))
			.fillColor(new Color(parent.config_fill_color, (float)parent.config_fill_opacity))
			.depthTestEnabled(parent.config_depth_test)
			.centerPosition()
			.build();

		// Existing markers will be overwritten.
		marker_sets.get(min.getWorld().getUID()).getMarkers().put(region.id().toString(), marker);
	}

	public void remove_marker(final UUID region_id) {
		for (final var marker_set : marker_sets.values()) {
			marker_set.getMarkers().remove(region_id.toString());
		}
	}

	public void update_all_markers() {
		for (final var region : get_module().all_regions()) {
			update_marker(region);
		}
	}
}
