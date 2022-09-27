package org.oddlama.vane.core.map.pl3x;

import net.pl3x.map.Key;
import net.pl3x.map.event.Event;
import net.pl3x.map.event.RegisteredHandler;
import net.pl3x.map.markers.option.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlexMapCreateAccessorRequestEvent extends Event {
	private static final List<RegisteredHandler> handlers = new ArrayList<>();

	private final String icon;
	private final Key icon_key;
	private final Key layer_key;
	private final Tooltip tooltip;
	private final String label_provider;

	public PlexMapCreateAccessorRequestEvent(@Nullable String icon,
											 @Nullable Key icon_key,
											 @NotNull Key layer_key,
											 @NotNull Tooltip tooltip,
											 @NotNull String label_provider) {
		this.icon = icon;
		this.icon_key = icon_key;
		this.layer_key = layer_key;
		this.tooltip = tooltip;
		this.label_provider = label_provider;
	}

	@Override
	public @NotNull List<RegisteredHandler> getHandlers() {
		return handlers;
	}

	public @Nullable String get_icon() {
		return icon;
	}

	public @Nullable Key get_icon_key() {
		return icon_key;
	}

	public @NotNull Key get_layer_key() {
		return layer_key;
	}

	public @NotNull Tooltip get_tooltip() {
		return tooltip;
	}

	public @NotNull String get_label_provider() {
		return label_provider;
	}

}
