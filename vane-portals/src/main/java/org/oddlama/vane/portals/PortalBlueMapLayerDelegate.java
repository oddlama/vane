package org.oddlama.vane.portals;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.portals.portal.Portal;

public class PortalBlueMapLayerDelegate {

	public static final String MARKER_SET_ID = "vane_portals.portals";

	private PortalBlueMapLayer parent = null;

	private boolean bluemap_enabled = false;

	public PortalBlueMapLayerDelegate(final PortalBlueMapLayer parent) {
		this.parent = parent;
	}

	public Portals get_module() {
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

	private String id_for(final BlueMapMap map, final UUID portal_id) {
		return map.getId() + ":" + portal_id.toString();
	}

	private String id_for(final BlueMapMap map, final Portal portal) {
		return id_for(map, portal.id());
	}

	public void update_marker(final Portal portal) {
		if (!bluemap_enabled) {
			return;
		}

		// Don't show private portals
		if (portal.visibility() == Portal.Visibility.PRIVATE) {
			remove_marker(portal.id());
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					update_marker_no_save(api, get_or_create_marker_set(marker_api), portal);
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating portal marker. Skipping.");
				}
			});
	}

	private void update_marker_no_save(final BlueMapAPI api, final MarkerSet marker_set, final Portal portal) {
		final var loc = portal.spawn();

		// Existing markers will be overwritten.
		final var bm_world = api.getWorld(loc.getWorld().getUID());
		if (bm_world.isPresent()) {
			for (final var map : bm_world.get().getMaps()) {
				final var marker = marker_set.createHtmlMarker(
					id_for(map, portal),
					map,
					loc.getX() + 0.5,
					loc.getY() + 0.5,
					loc.getZ() + 0.5,
					parent.lang_marker_label.str(escapeHtml(portal.name()))
				);
			}
		}
	}

	public void remove_marker(final UUID portal_id) {
		if (!bluemap_enabled) {
			return;
		}

		BlueMapAPI
			.getInstance()
			.ifPresent(api -> {
				try {
					final var marker_api = api.getMarkerAPI();
					for (final var map : api.getMaps()) {
						get_or_create_marker_set(marker_api).removeMarker(id_for(map, portal_id));
					}
					marker_api.save();
				} catch (IOException e) {
					get_module()
						.log.warning("Could not retrieve BlueMap marker api when updating portal marker. Skipping.");
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
					for (final var portal : get_module().all_available_portals()) {
						final var bm_world = api.getWorld(portal.spawn().getWorld().getUID());
						if (bm_world.isPresent()) {
							for (final var map : bm_world.get().getMaps()) {
								id_set.add(id_for(map, portal));
							}
						}
						update_marker_no_save(api, marker_set, portal);
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
						.log.warning("Could not retrieve BlueMap marker api when updating portal marker. Skipping.");
				}
			});
	}
}
