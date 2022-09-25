package org.oddlama.vane.core.map.pl3x;

import net.pl3x.map.Key;
import net.pl3x.map.event.Event;
import net.pl3x.map.event.RegisteredHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlexMapDisableAccessorEvent extends Event {
	private static final List<RegisteredHandler> handlers = new ArrayList<>();

	private final Key layer_key;

	public PlexMapDisableAccessorEvent(@NotNull Key layer_key) {
		this.layer_key = layer_key;
	}

	@Override
	public @NotNull List<RegisteredHandler> getHandlers() {
		return handlers;
	}

	public Key get_layer_key() {
		return layer_key;
	}

}
