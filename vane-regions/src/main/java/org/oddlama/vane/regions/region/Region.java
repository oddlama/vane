package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.regions.Regions;

public class Region {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var region = (Region) o;
        final var json = new JSONObject();
        json.put("id", to_json(UUID.class, region.id));
        json.put("name", to_json(String.class, region.name));
        json.put("owner", to_json(UUID.class, region.owner));
        json.put("region_group", to_json(UUID.class, region.region_group));
        json.put("extent", to_json(RegionExtent.class, region.extent));
        return json;
    }

    @SuppressWarnings("unchecked")
    public static Region deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var region = new Region();
        region.id = from_json(UUID.class, json.get("id"));
        region.name = from_json(String.class, json.get("name"));
        region.owner = from_json(UUID.class, json.get("owner"));
        region.region_group = from_json(UUID.class, json.get("region_group"));
        region.extent = from_json(RegionExtent.class, json.get("extent"));
        return region;
    }

    private Region() {}

    public Region(final String name, final UUID owner, final RegionExtent extent, final UUID region_group) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.extent = extent;
        this.region_group = region_group;
    }

    private UUID id;
    private String name;
    private UUID owner;
    private RegionExtent extent;
    private UUID region_group;

    public boolean invalidated = true;

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(final String name) {
        this.name = name;
        this.invalidated = true;
    }

    public UUID owner() {
        return owner;
    }

    public RegionExtent extent() {
        return extent;
    }

    private RegionGroup cached_region_group = null;

    public UUID region_group_id() {
        return region_group;
    }

    public void region_group_id(final UUID region_group) {
        this.region_group = region_group;
        this.cached_region_group = null;
        this.invalidated = true;
    }

    public RegionGroup region_group(final Regions regions) {
        if (cached_region_group == null) {
            cached_region_group = regions.get_region_group(region_group);
        }
        return cached_region_group;
    }
}
