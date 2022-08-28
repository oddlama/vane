package org.oddlama.vane.bedtime;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class BedtimeBlueMapLayerDelegate {

	public static final String MARKER_SET_ID = "vane_bedtime.bedtime";

	private final BedtimeBlueMapLayer parent;

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

	public void update_marker(final OfflinePlayer player) {
		remove_marker(player.getUniqueId());
		final var loc = player.getBedSpawnLocation();
		if (loc == null) {
			return;
		}

		final var marker = HtmlMarker.builder()
			.position((int)loc.getX(), (int)loc.getY(), (int)loc.getZ())
			.label("Bed for " + player.getName())
			.html(parent.lang_marker_label.str(escapeHtml(player.getName())))
			.build();

		// Existing markers will be overwritten.
		marker_sets.get(loc.getWorld().getUID()).getMarkers().put(player.getUniqueId().toString(), marker);
	}

	public void remove_marker(final UUID player_id) {
		for (final var marker_set : marker_sets.values()) {
			marker_set.getMarkers().remove(player_id.toString());
		}
	}

	public void update_all_markers() {
		// Update all existing
		for (final var player : get_module().get_offline_players_with_valid_name()) {
			update_marker(player);
		}
	}
}
