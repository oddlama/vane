package org.oddlama.vane.regions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.Shape;
import java.awt.Color;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.region.Region;

public class RegionBlueMapLayerDelegate {

	public static final String MARKER_SET_ID = "vane_regions.regions";

	private RegionBlueMapLayer parent = null;

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

	private MarkerSet get_or_create_marker_set(MarkerAPI api) {
		// createMarkerSet returns the existing set if any.
		final var marker_set = api.createMarkerSet(MARKER_SET_ID);

		// Update attributes
		marker_set.setLabel(parent.lang_layer_label.str());
		marker_set.setToggleable(true);
		marker_set.setDefaultHidden(parent.config_hide_by_default);

		return marker_set;
	}

	private String id_for(final BlueMapMap map, final UUID region_id) {
		return map.getId() + ":" + region_id.toString();
	}

	private String id_for(final BlueMapMap map, final Region region) {
		return id_for(map, region.id());
	}

	public void update_marker(final Region region) {
		if (!bluemap_enabled) {
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					update_marker_no_save(api, get_or_create_marker_set(marker_api), region);
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating region marker. Skipping.");
				}
			});
	}

	public void remove_marker(final UUID region_id) {
		if (!bluemap_enabled) {
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					for (final var map : api.getMaps()) {
						get_or_create_marker_set(marker_api).removeMarker(id_for(map, region_id));
					}
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating region marker. Skipping.");
				}
			});
	}

	private void update_marker_no_save(final BlueMapAPI api, final MarkerSet marker_set, final Region region) {
		final var min = region.extent().min();
		final var max = region.extent().max();
		final var shape = Shape.createRect(min.getX(), min.getZ(), max.getX() + 1, max.getZ() + 1);

		// Existing markers will be overwritten.
		final var bm_world = api.getWorld(min.getWorld().getUID());
		if (bm_world.isPresent()) {
			for (final var map : bm_world.get().getMaps()) {
				final var marker = marker_set.createExtrudeMarker(
					id_for(map, region),
					map,
					shape,
					min.getY(),
					max.getY() + 1
				);
				marker.setLineWidth(parent.config_line_width);
				marker.setLineColor(
					new Color(parent.config_line_color | ((int) (parent.config_line_opacity * 255.999) << 24), true)
				);
				marker.setFillColor(
					new Color(parent.config_fill_color | ((int) (parent.config_fill_opacity * 255.999) << 24), true)
				);
				marker.setDepthTestEnabled(parent.config_depth_test);
				marker.setLabel(parent.lang_marker_label.str(region.name()));
			}
		}
	}

	public void update_all_markers() {
		if (!bluemap_enabled) {
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					final var marker_set = get_or_create_marker_set(marker_api);

					// Update all existing
					final var id_set = new HashSet<String>();
					for (final var region : get_module().all_regions()) {
						final var bm_world = api.getWorld(region.extent().min().getWorld().getUID());
						if (bm_world.isPresent()) {
							for (final var map : bm_world.get().getMaps()) {
								id_set.add(id_for(map, region));
							}
						}
						update_marker_no_save(api, marker_set, region);
					}

					// Remove orphaned
					for (final var marker : marker_set.getMarkers()) {
						final var id = marker.getId();
						if (id != null && !id_set.contains(id)) {
							marker_set.removeMarker(marker.getId());
						}
					}

					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating region marker. Skipping.");
				}
			});
	}
}
