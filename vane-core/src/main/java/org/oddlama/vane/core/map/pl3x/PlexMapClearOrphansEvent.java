package org.oddlama.vane.core.map.pl3x;

import net.pl3x.map.Key;
import net.pl3x.map.event.Event;
import net.pl3x.map.event.RegisteredHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PlexMapClearOrphansEvent extends Event {
	private static final List<RegisteredHandler> handlers = new ArrayList<>();

	private final Key layer_key;
	private final HashSet<Key> active_keys;

	public PlexMapClearOrphansEvent(@NotNull Key layer_key,
									@NotNull HashSet<Key> active_keys) {
		this.layer_key = layer_key;
		this.active_keys = active_keys;
	}

	@Override
	public @NotNull List<RegisteredHandler> getHandlers() {
		return handlers;
	}

	public @NotNull Key get_layer_key() {
		return layer_key;
	}

	public @NotNull HashSet<Key> get_active_keys() {
		return active_keys;
	}

}
