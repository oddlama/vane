package org.oddlama.vane.core.map.pl3x;

import net.pl3x.map.Key;
import net.pl3x.map.event.Event;
import net.pl3x.map.event.RegisteredHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlexMapRemoveMarkerEvent extends Event {
	private static final List<RegisteredHandler> handlers = new ArrayList<>();

	private final UUID marker_id;
	private final Key layer_key;

	public PlexMapRemoveMarkerEvent(@NotNull UUID marker_id,
									@NotNull Key layer_key) {
		this.marker_id = marker_id;
		this.layer_key = layer_key;
	}

	@Override
	public @NotNull List<RegisteredHandler> getHandlers() {
		return handlers;
	}

	public @NotNull UUID get_marker_id() {
		return marker_id;
	}

	public Key get_layer_key() {
		return layer_key;
	}

}
