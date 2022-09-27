package org.oddlama.vane.bedtime;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.markers.Point;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.markers.option.Tooltip;
import org.bukkit.OfflinePlayer;
import org.oddlama.vane.core.map.pl3x.*;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class BedtimePlexMapLayerDelegate {

	private static final Key LAYER_KEY = Key.of("vane-bedtime-layer");
	private static final Key ICON_KEY = Key.of("vane-bedtime-icon");
	private static final Tooltip TOOLTIP = new Tooltip(
			"""
            <ul>
              <li>
                <name>
              </li>
            </ul>"""
	)
			.setDirection(Tooltip.Direction.RIGHT)
			.setPermanent(true)
			.setOffset(Point.of(5, 0));

	private final BedtimePlexMapLayer parent;
	private boolean plexmap_enabled = false;

	public BedtimePlexMapLayerDelegate(final BedtimePlexMapLayer parent) {
		this.parent = parent;
	}

	public Bedtime get_module() {
		return parent.get_module();
	}

	private String id_for(final UUID player_id) {
		return player_id.toString();
	}

	public void on_enable() {
		final var addon = Pl3xMap.api().getAddonRegistry().get(Key.of("Vane-Pl3xMap"));
		if (addon == null) {
			return;
		}

		get_module().log.info("Enabling Pl3xMap integration");

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapCreateAccessorRequestEvent(
				parent.config_marker_icon,
				ICON_KEY,
				LAYER_KEY,
				TOOLTIP,
				"Vane Bedtime"
		));

		if (!addon.isEnabled()) {
			get_module().log.log(Level.WARNING, "Error while enabling Pl3xMap integration!");
			return;
		}

		plexmap_enabled = true;
		update_all_markers();
	}

	public void on_disable() {
		if (plexmap_enabled) {
			Pl3xMap.api().getEventRegistry().callEvent(new PlexMapDisableAccessorEvent(LAYER_KEY));
			plexmap_enabled = false;
		}
	}

	public boolean update_marker(final OfflinePlayer player) {
		if (!plexmap_enabled) {
			return false;
		}

		final var loc = player.getBedSpawnLocation();
		if (loc == null) {
			return false;
		}

		final var world_name = loc.getWorld().getName();
		final var marker_label = player.getName();
		if (marker_label == null) {
			return false;
		}

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapUpdateMarkerEvent(
				LAYER_KEY,
				world_name,
				player.getUniqueId(),
				marker_label,
				new String[][] {
						{ "<name>", marker_label }
				},
				Marker.icon(LAYER_KEY, Point.of(loc.getX(), loc.getZ()), ICON_KEY)
		));

		return true;
	}

	public void remove_marker(final UUID player_id) {
		if (plexmap_enabled) {
			Pl3xMap.api().getEventRegistry().callEvent(new PlexMapRemoveMarkerEvent(
					player_id,
					LAYER_KEY
			));
		}
	}

	public void update_all_markers() {
		if (plexmap_enabled) {
			// Update all existing
			final var id_set = new HashSet<Key>();
			for (final var player : get_module().get_offline_players_with_valid_name()) {
				if (update_marker(player)) {
					id_set.add(Key.of(player.getUniqueId()));
				}
			}

			// Remove orphaned
			Pl3xMap.api().getEventRegistry().callEvent(new PlexMapClearOrphansEvent(
					LAYER_KEY,
					id_set
			));
		}
	}

	public boolean is_enabled() {
		return plexmap_enabled;
	}
}
