package org.oddlama.vane.regions.menu

import java.util.*

/** Lightweight menu tag carrying a selected region-group id. */
class RegionGroupMenuTag(private val regionGroupId: UUID?) {
    /** Returns the associated region-group id. */
    fun regionGroupId(): UUID? = regionGroupId
}
