package org.oddlama.vane.plexmap;

import net.pl3x.map.Key;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.addon.Addon;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.event.EventHandler;
import net.pl3x.map.event.EventListener;
import net.pl3x.map.event.world.WorldLoadedEvent;
import net.pl3x.map.event.world.WorldUnloadedEvent;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.world.World;
import org.oddlama.vane.core.map.pl3x.*;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class PlexMapAddon extends Addon implements EventListener {
	final LinkedHashMap<Key, PlexMapAccessor> accessors = new LinkedHashMap<>();
	final HashSet<World> loaded_worlds = new HashSet<>();

	private boolean plexmap_enabled = false;

	@Override
	public void onEnable() {
		Pl3xMap.api().getConsole().send("Enabling Vane Pl3xMap integration");

		try {
			Class.forName("org.oddlama.vane.core.map.pl3x.PlexMapUpdateMarkerEvent");
		} catch (ClassNotFoundException e) {
			Pl3xMap.api().getConsole().send("Failed to initialize Vane-Pl3xMap! Missing vane-core plugin!");
			this.onDisable();
			return;
		}

		// Register default icons
		FileUtil.extract(getClass(), "portal-default.png", "web/images/icon/", !Config.WEB_DIR_READONLY);
		FileUtil.extract(getClass(), "bedtime-default.png", "web/images/icon/", !Config.WEB_DIR_READONLY);

		// register event listener
		Pl3xMap.api().getEventRegistry().register(this);
		plexmap_enabled = true;
	}

	@Override
	public void onDisable() {
		if (!plexmap_enabled) {
			return;
		}

		if (accessors.isEmpty()) {
			Pl3xMap.api().getConsole().send("Disabling Vane Pl3xMap integration");
			plexmap_enabled = false;
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void load_event(WorldLoadedEvent event) {
		World world = event.getWorld();
		loaded_worlds.add(world);

		for (var accessor : accessors.values()) {
			accessor.on_world_load(world);
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void unload_event(WorldUnloadedEvent event) {
		final var world = event.getWorld();
		loaded_worlds.remove(world);

		for (final var accessor : accessors.values()) {
			world.getLayerRegistry().unregister(accessor.get_layer_key());
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void create_accessor(PlexMapCreateAccessorRequestEvent event) {
		final var accessor = new PlexMapAccessor(
				this,
				event.get_icon(),
				event.get_icon_key(),
				event.get_layer_key(),
				event.get_tooltip(),
				event.get_label_provider()
		);

		accessors.put(event.get_layer_key(), accessor);
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void update_request(PlexMapUpdateMarkerEvent event) {
		final var accessor = accessors.get(event.get_layer_key());
		if (accessor != null) {
			accessor.update_marker(
					event.get_world_key(),
					event.get_marker_id(),
					event.get_tooltip_replacements(),
					event.get_marker());
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void remove_request(PlexMapRemoveMarkerEvent event) {
		final var accessor = accessors.get(event.get_layer_key());
		if (accessor != null) {
			accessor.remove_marker(Key.of(event.get_marker_id()));
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void clear_orphans(PlexMapClearOrphansEvent event) {
		final var accessor = accessors.get(event.get_layer_key());
		if (accessor != null) {
			accessor.retain_in_set(event.get_active_keys());
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void disable_accessor(PlexMapDisableAccessorEvent event) {
		final var accessor = accessors.get(event.get_layer_key());
		if (accessor != null) {
			accessor.disable_module();
		}
	}

}
