package org.oddlama.vane.core.map.pl3x;

import net.pl3x.map.Key;
import net.pl3x.map.event.Event;
import net.pl3x.map.event.RegisteredHandler;
import net.pl3x.map.markers.marker.Marker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlexMapUpdateMarkerEvent extends Event {
	private static final List<RegisteredHandler> handlers = new ArrayList<>();

	private final Key layer_key;
	private final Key world;
	private final UUID marker_id;
	private final String marker_name;
	private final String[][] tooltip_replacements;
	private final Marker<?> marker;

	public PlexMapUpdateMarkerEvent(@NotNull Key layer_key,
									@NotNull String world,
									@NotNull UUID marker_id,
									@NotNull String marker_name,
									@NotNull String[][] tooltip_replacements,
									@NotNull Marker<?> marker) {
		this.layer_key = layer_key;
		this.world = Key.of(world);
		this.marker_id = marker_id;
		this.marker_name = marker_name;
		this.tooltip_replacements = tooltip_replacements;
		this.marker = marker;
	}

	@Override
	public @NotNull List<RegisteredHandler> getHandlers() {
		return handlers;
	}

	public Key get_layer_key() {
		return layer_key;
	}

	public @NotNull Key get_world_key() {
		return world;
	}

	public @NotNull UUID get_marker_id() {
		return marker_id;
	}

	public @NotNull String get_marker_name() {
		return marker_name;
	}

	public @NotNull String[][] get_tooltip_replacements() {
		return tooltip_replacements;
	}

	public @NotNull Marker<?> get_marker() {
		return marker;
	}

}
