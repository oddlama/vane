package org.oddlama.vane.regions.menu

import java.util.*

/** Lightweight menu tag carrying a selected region id. */
class RegionMenuTag(private val regionId: UUID?) {
    /** Returns the associated region id. */
    fun regionId(): UUID? = regionId
}
