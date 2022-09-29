package org.oddlama.vane.plexmap;

import net.pl3x.map.Key;
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

	public MarkerSet(String icon_filename, Key icon_key, Key layer_key) throws IOException {
		Path icon = World.WEB_DIR.resolve("images/icon/" + icon_filename);
		this.image = new IconImage(icon_key, ImageIO.read(icon.toFile()), "png");
		this.layer_key = layer_key;
	}

	public MarkerSet(Key layer_key) {
		this.layer_key = layer_key;
	}

	public @Nullable IconImage get_image() {
		return image;
	}

	public @NotNull Key get_layer_key() {
		return layer_key;
	}

	public @NotNull Collection<net.pl3x.map.markers.marker.Marker<?>> get_markers(Key world) {
		final var world_markers = MARKERS.get(world);
		if (world_markers == null) {
			return List.of();
		}

		return world_markers.stream().toList();
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
