package org.oddlama.vane.regions;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.markers.Point;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.markers.option.Tooltip;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.core.map.pl3x.*;
import org.oddlama.vane.regions.region.Region;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class RegionPlexMapLayerDelegate {

	private static final Key LAYER_KEY = Key.of("vane-regions-layer");
	private static final Tooltip TOOLTIP = new Tooltip(
			"""
            <ul>
              <li>
                <name><br />
                Owned by: <owner><br />
              </li>
            </ul>"""
	)
			.setDirection(Tooltip.Direction.RIGHT)
			.setPermanent(true)
			.setOffset(Point.of(5, 0));
	private final RegionPlexMapLayer parent;

	private boolean plexmap_enabled = false;

	public RegionPlexMapLayerDelegate(final RegionPlexMapLayer parent) {
		this.parent = parent;
	}

	public Regions get_module() {
		return parent.get_module();
	}

	public void on_enable(final Plugin plugin) {
		final var addon = Pl3xMap.api().getAddonRegistry().get(Key.of("Vane-Pl3xMap"));
		if (addon == null) {
			return;
		}

		get_module().log.info("Enabling Pl3xMap integration");

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapCreateAccessorRequestEvent(
				null,
				null,
				LAYER_KEY,
				TOOLTIP,
				"Vane Regions"
		));

		if (!addon.isEnabled()) {
			get_module().log.log(Level.WARNING, "Error while enabling Pl3xMap integration!");
			return;
		}

		plexmap_enabled = true;
		update_all_markers();
	}

	public void on_disable() {
		if (!plexmap_enabled) {
			return;
		}

		get_module().log.info("Disabling Pl3xMap integration");

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapDisableAccessorEvent(LAYER_KEY));
		plexmap_enabled = false;
	}

	private String id_for(final UUID region_id) {
		return region_id.toString();
	}

	private String id_for(final Region region) {
		return id_for(region.id());
	}

	public void update_marker(final Region region) {
		if (!plexmap_enabled) {
			return;
		}

		// Area markers can't be updated.
		remove_marker(region.id());

		final var min = region.extent().min();
		final var max = region.extent().max();
		final var world_name = min.getWorld().getName();
		//final var marker_label = parent.lang_marker_label.str(region.name());

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapUpdateMarkerEvent(
				LAYER_KEY,
				world_name,
				region.id(),
				"Region " + region.name(),
				new String[][] {
						{ "<name>", region.name() },
						{ "<owner>", get_module().getServer().getOfflinePlayer(region.owner()).getName() },
				},
				Marker.rectangle(
						Key.of(region.id()),
						Point.of(min.getX(), min.getZ()),
						Point.of(max.getX() + 1, max.getZ() + 1)
				)
		));
	}

	public void remove_marker(final UUID region_id) {
		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapRemoveMarkerEvent(
				region_id,
				LAYER_KEY
		));
	}

	public void update_all_markers() {
		if (!plexmap_enabled) {
			return;
		}

		// Update all existing
		final var id_set = new HashSet<Key>();
		for (final var region : get_module().all_regions()) {
			id_set.add(Key.of(id_for(region)));
			update_marker(region);
		}

		// Remove orphaned
		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapClearOrphansEvent(
				LAYER_KEY,
				id_set
		));
	}
}
