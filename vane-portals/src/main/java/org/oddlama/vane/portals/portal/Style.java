package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.util.StorageUtil;

public class Style {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var style = (Style) o;
        final var json = new JSONObject();
        json.put("key", to_json(NamespacedKey.class, style.key));
        try {
            json.put(
                "active_materials",
                to_json(Style.class.getDeclaredField("active_materials"), style.active_materials)
            );
            json.put(
                "inactive_materials",
                to_json(Style.class.getDeclaredField("inactive_materials"), style.inactive_materials)
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static Style deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var style = new Style(null);
        style.key = from_json(NamespacedKey.class, json.get("key"));
        try {
            style.active_materials = (Map<PortalBlock.Type, Material>) from_json(
                Style.class.getDeclaredField("active_materials"),
                json.get("active_materials")
            );
            style.inactive_materials = (Map<PortalBlock.Type, Material>) from_json(
                Style.class.getDeclaredField("inactive_materials"),
                json.get("inactive_materials")
            );
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Invalid field. This is a bug.", e);
        }
        return style;
    }

    private NamespacedKey key;
    private Map<PortalBlock.Type, Material> active_materials = new HashMap<>();
    private Map<PortalBlock.Type, Material> inactive_materials = new HashMap<>();

    public Style(final NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

    public Material material(boolean active, PortalBlock.Type type) {
        if (active) {
            return active_materials.get(type);
        } else {
            return inactive_materials.get(type);
        }
    }

    public static NamespacedKey default_style_key() {
        return StorageUtil.namespaced_key("vane_portals", "portal_style_default");
    }

    public void set_material(boolean active, PortalBlock.Type type, Material material) {
        set_material(active, type, material, false);
    }

    public void set_material(boolean active, PortalBlock.Type type, Material material, boolean overwrite) {
        final Map<PortalBlock.Type, Material> map;
        if (active) {
            map = active_materials;
        } else {
            map = inactive_materials;
        }

        if (!overwrite && map.containsKey(type)) {
            throw new RuntimeException(
                "Invalid style definition! PortalBlock.Type." + type + " was specified multiple times."
            );
        }
        map.put(type, material);
    }

    public void check_valid() {
        // Checks if every key is set
        for (final var type : PortalBlock.Type.values()) {
            if (!active_materials.containsKey(type)) {
                throw new RuntimeException(
                    "Invalid style definition! Active state for PortalBlock.Type." + type + " was not specified!"
                );
            }
            if (!inactive_materials.containsKey(type)) {
                throw new RuntimeException(
                    "Invalid style definition! Inactive state for PortalBlock.Type." + type + " was not specified!"
                );
            }
        }
    }

    public static Style default_style() {
        final var style = new Style(default_style_key());
        style.set_material(true, PortalBlock.Type.BOUNDARY_1, Material.OBSIDIAN);
        style.set_material(true, PortalBlock.Type.BOUNDARY_2, Material.CRYING_OBSIDIAN);
        style.set_material(true, PortalBlock.Type.BOUNDARY_3, Material.GOLD_BLOCK);
        style.set_material(true, PortalBlock.Type.BOUNDARY_4, Material.GILDED_BLACKSTONE);
        style.set_material(true, PortalBlock.Type.BOUNDARY_5, Material.EMERALD_BLOCK);
        style.set_material(true, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE);
        style.set_material(true, PortalBlock.Type.ORIGIN, Material.OBSIDIAN);
        style.set_material(true, PortalBlock.Type.PORTAL, Material.END_GATEWAY);
        style.set_material(false, PortalBlock.Type.BOUNDARY_1, Material.OBSIDIAN);
        style.set_material(false, PortalBlock.Type.BOUNDARY_2, Material.CRYING_OBSIDIAN);
        style.set_material(false, PortalBlock.Type.BOUNDARY_3, Material.GOLD_BLOCK);
        style.set_material(false, PortalBlock.Type.BOUNDARY_4, Material.GILDED_BLACKSTONE);
        style.set_material(false, PortalBlock.Type.BOUNDARY_5, Material.EMERALD_BLOCK);
        style.set_material(false, PortalBlock.Type.CONSOLE, Material.ENCHANTING_TABLE);
        style.set_material(false, PortalBlock.Type.ORIGIN, Material.OBSIDIAN);
        style.set_material(false, PortalBlock.Type.PORTAL, Material.AIR);
        return style;
    }

    public Style copy(final NamespacedKey new_key) {
        final var copy = new Style(new_key);
        copy.active_materials = new HashMap<>(active_materials);
        copy.inactive_materials = new HashMap<>(inactive_materials);
        return copy;
    }
}
