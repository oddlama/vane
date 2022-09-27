package org.oddlama.vane.plexmap;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.image.IconImage;
import net.pl3x.map.markers.marker.Marker;
import net.pl3x.map.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class MarkerSet {

	private final Key layer_key;
	final LinkedHashMap<Key, HashSet<Marker<?>>> MARKERS = new LinkedHashMap<>();

	private IconImage image = null;
	private Key icon_key = null;

	public MarkerSet(String icon_filename, Key icon_key, Key layer_key) throws IOException {
		Path icon = World.WEB_DIR.resolve("images/icon/" + icon_filename);
		this.image = new IconImage(icon_key, ImageIO.read(icon.toFile()), "png");
		this.icon_key = icon_key;
		this.layer_key = layer_key;
	}

	public MarkerSet(Key layer_key) {
		this.layer_key = layer_key;
	}

	public @Nullable IconImage get_image() {
		return image;
	}

	public @Nullable Key get_icon_key() {
		return icon_key;
	}

	public @NotNull Key get_layer_key() {
		return layer_key;
	}

	public @NotNull Collection<net.pl3x.map.markers.marker.Marker<?>> get_markers(Key world) {
		Pl3xMap.api().getConsole().send("Getting markers for world " + world.toString());
		final var world_markers = MARKERS.get(world);
		if (world_markers == null) {
			Pl3xMap.api().getConsole().send("World " + world + " not found! Available worlds are: " + MARKERS.keySet());
			return List.of();
		}

		Pl3xMap.api().getConsole().send("Found " + world_markers.size() + " markers in world " + world + ": " + world_markers);
		return world_markers.stream().toList();
	}

	public @NotNull Collection<Marker<?>> get_markers() {
		if (MARKERS.isEmpty()) {
			return List.of();
		}

		HashSet<Marker<?>> markers = new HashSet<>();

		for (final var world_set : MARKERS.entrySet()) {
			markers.addAll(world_set.getValue());
		}

		return markers;
	}

	public void remove_marker(Key id) {
		for (var world_markers : MARKERS.entrySet()) {
			if (world_markers.getValue().removeIf(marker -> Objects.equals(marker.getKey(), id))) {
				break;
			}
		}
	}

	public void update_marker(Key world, Marker<?> marker) {
		var world_markers = MARKERS.get(world);
		if (world_markers != null) {
			world_markers.add(marker);
		}
	}

}
