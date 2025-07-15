package org.oddlama.vane.regions.menu;

import java.util.UUID;

public class RegionGroupMenuTag {

    private final UUID region_group_id;

    public RegionGroupMenuTag(final UUID region_group_id) {
        this.region_group_id = region_group_id;
    }

    public UUID region_group_id() {
        return region_group_id;
    }
}
