package org.oddlama.vane.bedtime;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class BedtimeBlueMapLayerDelegate {

	public static final String MARKER_SET_ID = "vane_bedtime.bedtime";

	private BedtimeBlueMapLayer parent = null;

	private boolean bluemap_enabled = false;

	public BedtimeBlueMapLayerDelegate(final BedtimeBlueMapLayer parent) {
		this.parent = parent;
	}

	public Bedtime get_module() {
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

	private String id_for(final BlueMapMap map, final UUID player_id) {
		return map.getId() + ":" + player_id.toString();
	}

	private String id_for(final BlueMapMap map, final OfflinePlayer player) {
		return id_for(map, player.getUniqueId());
	}

	public void update_marker(final OfflinePlayer player) {
		if (!bluemap_enabled) {
			return;
		}

		final var loc = player.getBedSpawnLocation();
		if (loc == null) {
			remove_marker(player.getUniqueId());
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					update_marker_no_save(api, get_or_create_marker_set(marker_api), player);
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating bed marker. Skipping.");
				}
			});
	}

	private void update_marker_no_save(final BlueMapAPI api, final MarkerSet marker_set, final OfflinePlayer player) {
		final var loc = player.getBedSpawnLocation();
		if (loc == null) {
			return;
		}

		// Existing markers will be overwritten.
		final var bm_world = api.getWorld(loc.getWorld().getUID());
		if (bm_world.isPresent()) {
			for (final var map : bm_world.get().getMaps()) {
				marker_set.createHtmlMarker(
					id_for(map, player),
					map,
					loc.getX() + 0.5,
					loc.getY() + 0.5,
					loc.getZ() + 0.5,
					parent.lang_marker_label.str(escapeHtml(player.getName()))
				);
			}
		}
	}

	public void remove_marker(final UUID player_id) {
		if (!bluemap_enabled) {
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					for (final var map : api.getMaps()) {
						get_or_create_marker_set(marker_api).removeMarker(id_for(map, player_id));
					}
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating bed marker. Skipping.");
				}
			});
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
					for (final var player : get_module().get_offline_players_with_valid_name()) {
						final var loc = player.getBedSpawnLocation();
						if (loc == null) {
							continue;
						}
						final var bm_world = api.getWorld(loc.getWorld().getUID());
						if (bm_world.isPresent()) {
							for (final var map : bm_world.get().getMaps()) {
								id_set.add(id_for(map, player));
							}
						}
						update_marker_no_save(api, marker_set, player);
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
						.log.warning("Could not retrieve BlueMap marker api when updating bed marker. Skipping.");
				}
			});
	}
}
