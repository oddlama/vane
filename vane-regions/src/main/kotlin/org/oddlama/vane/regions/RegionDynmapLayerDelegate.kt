package org.oddlama.vane.regions

import java.util.*
import org.bukkit.plugin.Plugin
import org.dynmap.DynmapCommonAPI
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerSet
import org.oddlama.vane.core.dynmap.DynmapIntegration
import org.oddlama.vane.regions.region.Region

/**
 * Low-level Dynmap API delegate used by `RegionDynmapLayer`.
 *
 * This delegate manages Dynmap registration, marker set creation and synchronization of region area
 * markers. It is intentionally lightweight and invoked by the owning `RegionDynmapLayer` when
 * Dynmap integration is enabled.
 *
 * @param parent The owning [RegionDynmapLayer] instance.
 * @constructor Creates a new delegate bound to the owning [parent] layer.
 */
class RegionDynmapLayerDelegate(private val parent: RegionDynmapLayer) {
    /** Active Dynmap API instance captured when integration becomes available. */
    private var dynmapApi: DynmapCommonAPI? = null

    /** Dynmap MarkerAPI instance used to create and manage markers. */
    private var markerApi: MarkerAPI? = null

    /** Whether Dynmap integration is currently active. */
    private var dynmapEnabled = false

    /** MarkerSet used for all region overlay markers; created or loaded on enable. */
    private var markerSet: MarkerSet? = null

    /** Convenience accessor for the owning [Regions] module. */
    private val module: Regions
        get() = requireNotNull(parent.module)

    /**
     * Called when the owning layer is enabled; registers Dynmap integration and creates/loads the
     * marker layer if Dynmap is available.
     *
     * @param plugin Optional plugin reference (unused). The parameter is kept to match the caller
     *   signature and for potential future use.
     */
    fun onEnable(@Suppress("UNUSED_PARAMETER") plugin: Plugin?) {
        if (!registerAndInitDynmap()) return
    }

    /**
     * Helper that registers the Dynmap listener and initializes the integration.
     *
     * The function delegates registration to [DynmapIntegration] and assigns the discovered API
     * handles to the delegate. When Dynmap is available the marker set is created/loaded via
     * [createOrLoadLayer].
     *
     * @return True when Dynmap integration is available and initialization proceeded.
     */
    private fun registerAndInitDynmap(): Boolean {
        return DynmapIntegration.initialize(
            module.log,
            { d, m ->
                dynmapApi = d
                markerApi = m
            },
        ) {
            dynmapEnabled = true
            createOrLoadLayer()
        }
    }

    /**
     * Disable Dynmap integration and clear internal handles.
     *
     * This resets the enabled flag and drops references to the Dynmap API and marker API to allow
     * GC and clean state for future reinitialization.
     */
    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        module.log.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    /**
     * Create or update the Dynmap marker set and apply the current configuration (labels, priority,
     * visibility). If the marker set or marker API cannot be resolved this function simply returns
     * without throwing.
     */
    private fun createOrLoadLayer() {
        val api = markerApi ?: return

        // Create or retrieve layer
        markerSet = api.getMarkerSet(RegionDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet =
                api.createMarkerSet(
                    RegionDynmapLayer.LAYER_ID,
                    // Use safe call with fallback for nullable TranslatedMessage
                    parent.langLayerLabel?.str() ?: "",
                    null,
                    false,
                )
        }

        if (markerSet == null) {
            module.log.severe("Failed to create dynmap region marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel?.str() ?: ""
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Initial update
        updateAllMarkers()
    }

    /**
     * Convert a region UUID to a Dynmap marker id string.
     *
     * @param regionId UUID of the region.
     * @return String identifier used for Dynmap markers.
     */
    private fun idFor(regionId: UUID): String {
        return regionId.toString()
    }

    /**
     * Resolve the marker id for a [region] instance.
     *
     * @param region Region instance to resolve.
     * @return Marker id string or null when the region has no id.
     */
    private fun idFor(region: Region): String? = region.id()?.let(::idFor)

    /**
     * Recreate or update the Dynmap area marker representing [region].
     *
     * If the marker set is unavailable or Dynmap integration is disabled this function returns
     * without action.
     *
     * @param region Region to create/update marker for.
     */
    fun updateMarker(region: Region) {
        if (!dynmapEnabled) {
            return
        }

        // Area markers can't be updated.
        val regionId = region.id() ?: return
        val extent = region.extent() ?: return
        removeMarker(regionId)

        val min = extent.min() ?: return
        val max = extent.max() ?: return
        val worldName = min.world.name
        val markerId = idFor(region) ?: return
        // Use safe call with fallback for nullable TranslatedMessage
        val markerLabel = parent.langMarkerLabel?.str(region.name()) ?: region.name()

        val markerSetRef = markerSet ?: return
        val xs = doubleArrayOf(min.x.toDouble(), (max.x + 1).toDouble())
        val zs = doubleArrayOf(min.z.toDouble(), (max.z + 1).toDouble())
        val area =
            markerSetRef.createAreaMarker(markerId, markerLabel, false, worldName, xs, zs, false)
        area.setRangeY((max.y + 1).toDouble(), min.y.toDouble())
        area.setLineStyle(parent.configLineWeight, parent.configLineOpacity, parent.configLineColor)
        area.setFillStyle(parent.configFillOpacity, parent.configFillColor)
    }

    /**
     * Remove a marker by its region id.
     *
     * @param regionId UUID of the region whose marker should be removed.
     */
    fun removeMarker(regionId: UUID) {
        removeMarker(idFor(regionId))
    }

    /**
     * Remove a marker by marker id string.
     *
     * @param markerId Identifier of the marker to remove, or null to do nothing.
     */
    fun removeMarker(markerId: String?) {
        if (!dynmapEnabled || markerId == null) return

        removeMarker(markerSet?.findMarker(markerId))
    }

    /**
     * Delete a concrete Dynmap [marker] instance when present.
     *
     * @param marker Marker instance to delete; no-op when null or integration disabled.
     */
    fun removeMarker(marker: Marker?) {
        if (!dynmapEnabled || marker == null) return

        marker.deleteMarker()
    }

    /**
     * Synchronize all region markers: recreate markers for every region and prune orphaned markers
     * that no longer correspond to an existing region.
     */
    fun updateAllMarkers() {
        if (!dynmapEnabled) {
            return
        }

        // Update all existing
        val markerSetRef = markerSet ?: return
        val idSet = HashSet<String>()
        for (region in module.allRegions()) {
            val r = region ?: continue
            val regionId = idFor(r) ?: continue
            idSet.add(regionId)
            updateMarker(r)
        }

        // Remove orphaned
        for (marker in markerSetRef.markers) {
            val id = marker.markerID
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker)
            }
        }
    }
}
