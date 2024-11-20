package org.oddlama.vane.regions.menu;

import java.util.UUID;

public class RegionMenuTag {

    private final UUID region_id;

    public RegionMenuTag(final UUID region_id) {
        this.region_id = region_id;
    }

    public UUID region_id() {
        return region_id;
    }
}
