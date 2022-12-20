package org.oddlama.vane.portals;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.markers.Point;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.markers.option.Tooltip;
import org.oddlama.vane.core.map.pl3x.*;
import org.oddlama.vane.portals.portal.Portal;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class PortalPlexMapLayerDelegate {

	private static final Key LAYER_KEY = Key.of("vane-portals-layer");
	private static final Key ICON_KEY = Key.of("vane-portals-icon");
	private static final Tooltip TOOLTIP = new Tooltip(
			"""
            <ul>
              <li>
                <name><br />
                Owned by: <owner><br />
                Linked to: <linked>
              </li>
            </ul>"""
	)
			.setDirection(Tooltip.Direction.RIGHT)
			.setPermanent(true)
			.setOffset(Point.of(5, 0));

	private final PortalPlexMapLayer parent;
	private boolean plexmap_enabled = false;

	public PortalPlexMapLayerDelegate(final PortalPlexMapLayer parent) {
		this.parent = parent;
	}

	public Portals get_module() {
		return parent.get_module();
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
				"Vane Portals"
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

	public boolean update_marker(final Portal portal) {
		if (!plexmap_enabled || portal.visibility() == Portal.Visibility.PRIVATE) {
			return false;
		}

		final var loc = portal.spawn();

		String target_name;
		final var target = get_module().portal_for(portal.target_id());
		if (target == null) {
			target_name = "None";
		} else {
			target_name = target.name();
		}

		String owner_name;
		final var owner = get_module().getServer().getPlayer(portal.owner());
		if (owner == null) {
			owner_name = "Unknown";
		} else {
			owner_name = owner.getName();
		}

		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapUpdateMarkerEvent(
				LAYER_KEY,
				loc.getWorld().getName(),
				portal.id(),
				"Portal " + portal.name(),
				new String[][] {
						{ "<name>", portal.name() },
						{ "<owner>", owner_name },
						{ "<linked>", target_name }
				},
				Marker.icon(LAYER_KEY, Point.of(loc.getX(), loc.getZ()), ICON_KEY)
		));

		return true;
	}

	public void remove_marker(final UUID portal_id) {
		if (plexmap_enabled) {
			Pl3xMap.api().getEventRegistry().callEvent(new PlexMapRemoveMarkerEvent(
					portal_id,
					LAYER_KEY
			));
		}
	}

	public void update_all_markers() {
		if (!plexmap_enabled) return;

		// Update all existing
		final var id_set = new HashSet<Key>();
		for (final var portal : get_module().all_available_portals()) {
			if (update_marker(portal)) {
				id_set.add(Key.of(portal.id()));
			}
		}

		// Remove orphaned
		Pl3xMap.api().getEventRegistry().callEvent(new PlexMapClearOrphansEvent(
				LAYER_KEY,
				id_set
		));
	}

	public boolean is_enabled() {
		return plexmap_enabled;
	}
}

