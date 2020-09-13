package org.oddlama.vane.portals.portal;

import org.oddlama.vane.portals.PortalConstructor;
import org.oddlama.vane.portals.Portals;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public class Style {
	private NamespacedKey key;
	private Map<PortalBlock.Type, Material> active_materials = new HashMap<>();
	private Map<PortalBlock.Type, Material> inactive_materials = new HashMap<>();

	private Style(final NamespacedKey key) {
		this.key = key;
	}

	public NamespacedKey key() {
		return key;
	}

	public Material material(PortalBlock.Type type, boolean active) {
		if (active) {
			return active_materials.get(type);
		} else {
			return inactive_materials.get(type);
		}
	}

	public static NamespacedKey default_style_key() {
		return namespaced_key("vane_portals", "portal_style_default");
	}

	public static Style default_style() {
		final var style = new Style(default_style_key());
		style.active_materials.put(PortalBlock.Type.BOUNDARY, Material.OBSIDIAN);
		style.active_materials.put(PortalBlock.Type.CONSOLE,  Material.ENCHANTING_TABLE);
		style.active_materials.put(PortalBlock.Type.ORIGIN,   Material.OBSIDIAN);
		style.active_materials.put(PortalBlock.Type.PORTAL,   Material.END_GATEWAY);
		style.inactive_materials.put(PortalBlock.Type.BOUNDARY, Material.OBSIDIAN);
		style.inactive_materials.put(PortalBlock.Type.CONSOLE,  Material.ENCHANTING_TABLE);
		style.inactive_materials.put(PortalBlock.Type.ORIGIN,   Material.OBSIDIAN);
		style.inactive_materials.put(PortalBlock.Type.PORTAL,   Material.AIR);
		return style;
	}
}
