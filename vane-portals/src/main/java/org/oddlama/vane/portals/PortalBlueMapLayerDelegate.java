package org.oddlama.vane.portals;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.portals.portal.Portal;

public class PortalBlueMapLayerDelegate {

    public static final String MARKER_SET_ID = "vane_portals.portals";

    private final PortalBlueMapLayer parent;

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

        api
            .getWorld(world)
            .ifPresent(bm_world -> {
                for (final var map : bm_world.getMaps()) {
                    map.getMarkerSets().put(MARKER_SET_ID, marker_set);
                }
            });

        marker_sets.put(world.getUID(), marker_set);
    }

    public void update_marker(final Portal portal) {
        remove_marker(portal.id());

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            return;
        }

        final var loc = portal.spawn();
        final var marker = HtmlMarker.builder()
            .position(loc.getX(), loc.getY(), loc.getZ())
            .label("Portal " + portal.name())
            .html(parent.lang_marker_label.str(escapeHtml(portal.name())))
            .build();

        // Existing markers will be overwritten.
        marker_sets.get(loc.getWorld().getUID()).getMarkers().put(portal.id().toString(), marker);
    }

    public void remove_marker(final UUID portal_id) {
        for (final var marker_set : marker_sets.values()) {
            marker_set.getMarkers().remove(portal_id.toString());
        }
    }

    public void update_all_markers() {
        for (final var portal : get_module().all_available_portals()) {
            // Don't show private portals
            if (portal.visibility() == Portal.Visibility.PRIVATE) {
                continue;
            }

            update_marker(portal);
        }
    }
}
