package org.oddlama.vane.regions;

import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.regions.region.Region;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RegionSquaremapLayerDelegate {
    private final RegionSquaremapLayer parent;
    private boolean enabled = false;
    private final HashMap<UUID, Key> markers = new HashMap<>();
    private final Map<World, SimpleLayerProvider> worldProviders = new HashMap<>();

    public RegionSquaremapLayerDelegate(final RegionSquaremapLayer parent) {
        this.parent = parent;
    }

    public Regions get_module() {
        return parent.get_module();
    }

    public void on_enable(final Plugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("squaremap")) {
            return;
        }

        get_module().log.info("Enabling squaremap integration");
        enabled = true;

        Squaremap squaremap = SquaremapProvider.get();

        for (World world : Bukkit.getWorlds()) {
            Optional<MapWorld> optionalMapWorld = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));

            if (optionalMapWorld.isPresent()) {
                MapWorld mapWorld = optionalMapWorld.get();
                Key layerKey = Key.of("vane_regions_" + world.getName());

                SimpleLayerProvider provider = SimpleLayerProvider.builder(parent.lang_layer_label.str())
                    .showControls(true)
                    .defaultHidden(parent.config_layer_hide)
                    .build();

                mapWorld.layerRegistry().register(layerKey, provider);
                worldProviders.put(world, provider);
            }
        }

        update_all_markers();
    }

    public void on_disable() {
        if (!enabled) {
            return;
        }

        get_module().log.info("Disabling squaremap integration");
        enabled = false;
        markers.clear();
        worldProviders.clear();
    }

    private Key id_for(final Region region) {
        return Key.of(region.id().toString());
    }

    public void update_marker(final Region region) {
        if (!enabled) {
            return;
        }

        remove_marker(region);

        var world = region.extent().min().getWorld();
        var min = region.extent().min();
        var max = region.extent().max();

        Key markerKey = id_for(region);

        Point p1 = Point.of(min.getX(), min.getZ());
        Point p2 = Point.of(max.getX() + 1, max.getZ() + 1);

        if (!worldProviders.containsKey(world)) {
            get_module().log.warning("Squaremap: No provider found for world " + world.getName() + " while updating marker.");
            return;
        }
        SimpleLayerProvider provider = worldProviders.get(world);
        
        Marker marker = Marker.rectangle(p1, p2);
        MarkerOptions options = MarkerOptions.builder()
            .fill(parent.config_layer_fill)
            .fillColor(new Color(parent.config_fill_color))
            .fillOpacity(parent.config_fill_opacity)
            .stroke(parent.config_line)
            .strokeColor(new Color(parent.config_line_color))
            .strokeWeight(parent.config_line_weight)
            .strokeOpacity(parent.config_line_opacity)
            .hoverTooltip(parent.lang_marker_label.str(region.name()))
            .clickTooltip(parent.lang_marker_label.str(region.name()))
            .build();

        marker.markerOptions(options);
        provider.addMarker(markerKey, marker);
        markers.put(region.id(), markerKey);
    }

    public void remove_marker(final Region region) {
        remove_marker(region.id());
    }

    public void remove_marker(final UUID regionId) {
        if (!enabled) return;
        Key key = markers.remove(regionId);
        if (key == null) return;

        for (SimpleLayerProvider provider : worldProviders.values()) {
            provider.removeMarker(key);
        }
    }

    public void update_all_markers() {
        if (!enabled) return;

        for (final var region : get_module().all_regions()) {
            update_marker(region);
        }
    }
}
