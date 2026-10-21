package org.oddlama.vane.portals

// ...existing imports...
import java.util.*
import org.dynmap.DynmapCommonAPI
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerIcon
import org.dynmap.markers.MarkerSet
import org.oddlama.vane.core.dynmap.DynmapIntegration
import org.oddlama.vane.portals.portal.Portal

// ...existing imports...

/** Internal Dynmap integration helper used by [PortalDynmapLayer]. */
class PortalDynmapLayerDelegate(private val parent: PortalDynmapLayer) {
    /** Cached Dynmap API handle. */
    private var dynmapApi: DynmapCommonAPI? = null

    /** Cached Dynmap marker API handle. */
    private var markerApi: MarkerAPI? = null

    /** Tracks whether Dynmap integration is currently active. */
    private var dynmapEnabled = false

    /** Marker set used to store portal markers. */
    private var markerSet: MarkerSet? = null

    /** Icon used when creating portal markers. */
    private var markerIcon: MarkerIcon? = null

    /** Returns the owning portals module. */
    val module: Portals?
        get() = parent.module

    /** Registers Dynmap listeners and initializes the portal layer. */
    fun onEnable() {
        val module = module ?: return
        if (!registerAndInitDynmap(module)) return
    }

    /**
     * Helper that registers the Dynmap listener and initializes the integration. Returns true when
     * dynmap integration was successfully enabled and initialization (including marker API
     * presence) completed.
     */
    private fun registerAndInitDynmap(module: Portals): Boolean {
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

    /** Disables Dynmap integration state. */
    fun onDisable() {
        if (!dynmapEnabled) {
            return
        }

        module?.log?.info("Disabling dynmap integration")
        dynmapEnabled = false
        dynmapApi = null
        markerApi = null
    }

    /** Creates or loads the marker layer and applies configuration. */
    private fun createOrLoadLayer() {
        val markerApi = markerApi ?: return

        // Create or retrieve layer
        markerSet =
            markerApi.getMarkerSet(PortalDynmapLayer.LAYER_ID)
                ?: markerApi.createMarkerSet(
                    PortalDynmapLayer.LAYER_ID,
                    parent.langLayerLabel!!.str(),
                    null,
                    false,
                )

        if (markerSet == null) {
            module?.log?.severe("Failed to create dynmap portal marker set!")
            return
        }

        // Update attributes
        markerSet!!.markerSetLabel = parent.langLayerLabel!!.str()
        markerSet!!.layerPriority = parent.configLayerPriority
        markerSet!!.hideByDefault = parent.configLayerHide

        // Load marker
        markerIcon = markerApi.getMarkerIcon(parent.configMarkerIcon)
        if (markerIcon == null) {
            module?.log?.severe("Failed to load dynmap portal marker icon!")
            return
        }

        // Initial update
        updateAllMarkers()
    }

    /** Converts a portal id into the Dynmap marker id format. */
    private fun idFor(portalId: UUID?) = portalId?.toString()

    /** Returns the marker id for [portal]. */
    private fun idFor(portal: Portal) = idFor(portal.id())

    /** Updates or creates the Dynmap marker for [portal]. */
    fun updateMarker(portal: Portal) {
        if (!dynmapEnabled) {
            return
        }

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            removeMarker(portal.id())
            return
        }

        val loc = portal.spawn()
        val worldName = loc.world.name
        val markerId = idFor(portal) ?: return
        val markerLabel = parent.langMarkerLabel!!.str(portal.name())

        markerSet!!.createMarker(
            markerId,
            markerLabel,
            worldName,
            loc.x,
            loc.y,
            loc.z,
            markerIcon,
            false,
        )
    }

    /** Removes a marker by portal id. */
    fun removeMarker(portalId: UUID?) {
        removeMarker(idFor(portalId))
    }

    /** Removes a marker by marker id. */
    fun removeMarker(markerId: String?) {
        if (!dynmapEnabled || markerId == null) return

        removeMarker(markerSet!!.findMarker(markerId))
    }

    /** Removes the provided marker instance. */
    fun removeMarker(marker: Marker?) {
        if (!dynmapEnabled || marker == null) return

        marker.deleteMarker()
    }

    /** Rebuilds all Dynmap markers and removes orphaned entries. */
    fun updateAllMarkers() {
        if (!dynmapEnabled) {
            return
        }

        // Update all existing
        val idSet = HashSet<String?>()
        for (portal in module?.allAvailablePortals().orEmpty().filterNotNull()) {
            idSet.add(idFor(portal))
            updateMarker(portal)
        }

        // Remove orphaned
        for (marker in markerSet!!.markers) {
            val id = marker.markerID
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker)
            }
        }
    }
}
