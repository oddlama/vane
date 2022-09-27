package org.oddlama.vane.plexmap;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.markers.layer.SimpleLayer;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.markers.option.Options;
import net.pl3x.map.markers.option.Tooltip;
import net.pl3x.map.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlexMapAccessor {

	private final PlexMapAddon addon;
	private final MarkerSet set;
	private final Key layer_key;
	private final Tooltip tooltip;
	private final String label_provider;

	public PlexMapAccessor(PlexMapAddon addon,
						   String icon,
						   Key icon_key,
						   Key layer_key,
						   Tooltip tooltip,
						   String label_provider) {
		this.addon = addon;
		this.layer_key = layer_key;
		this.tooltip = tooltip;
		this.label_provider = label_provider;

		MarkerSet set;
		if (icon == null) {
			set = new MarkerSet(layer_key);
		} else {
			try {
				set = new MarkerSet(icon, icon_key, layer_key);
				Pl3xMap.api().getIconRegistry().register(set.get_image());
			} catch (Exception ignored) {
				throw new IllegalStateException("Failed to load Vane Pl3xMap marker icon!");
			}
		}

		this.set = set;
		init_markers(addon.loaded_worlds);
	}

	public @NotNull Key get_layer_key() {
		return layer_key;
	}

	public void init_markers(HashSet<World> worlds) {
		for (final var world : worlds) {
			on_world_load(world);
		}
	}

	public void on_world_load(World world) {
		set.MARKERS.put(world.getKey(), new HashSet<>());

		world.getLayerRegistry().register(new SimpleLayer(set.get_layer_key(), () -> label_provider) {
					@Override
					public @NotNull Collection<Marker<?>> getMarkers() {
						return set.get_markers(world.getKey());
					}
				}
						.setPriority(999)
		);
	}

	public void update_marker(Key world, UUID id, String[][] tooltip_replacements, Marker<?> marker) {
		remove_marker(Key.of(id));

		var tooltip_content = tooltip.getContent();

		for (final var replacements : tooltip_replacements) {
			tooltip_content = tooltip_content.replace(replacements[0], replacements[1]);
		}

		marker.setOptions(Options.builder()
				.tooltipContent(tooltip_content)
				.tooltipDirection(tooltip.getDirection())
				.tooltipOffset(tooltip.getOffset())
				.tooltipOpacity(1.0D)
				.build());

		set.update_marker(world, marker);
	}

	public void remove_marker(final Key id) {
		set.remove_marker(id);
	}

	public void retain_in_set(final HashSet<Key> valid_ids) {
		for (var world_marker_set : set.MARKERS.values()) {
			world_marker_set.removeIf(it -> !valid_ids.contains(it.getKey()));
		}
	}

	public void disable_module() {
		addon.accessors.remove(layer_key);
		addon.onDisable();
	}

}
