package org.oddlama.vane.bedtime

import java.util.*
import org.bukkit.OfflinePlayer
import org.dynmap.DynmapCommonAPI
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerIcon
import org.dynmap.markers.MarkerSet
import org.oddlama.vane.core.dynmap.DynmapIntegration

/**
 * Handles Dynmap API integration and bedtime marker synchronization.
 *
 * @param parent owning Dynmap layer component.
 * @constructor Creates a Dynmap integration delegate bound to its parent layer.
 */
class BedtimeDynmapLayerDelegate(private val parent: BedtimeDynmapLayer) {
    /** Live reference to the Dynmap common API; `null` until `apiEnabled` fires. */
    private var dynmapApi: DynmapCommonAPI? = null

    /** Marker API obtained from [dynmapApi]; `null` until the API is ready. */
    private var markerApi: MarkerAPI? = null

    /** Whether Dynmap integration is currently active. */
    private var dynmapEnabled = false

    /** The marker set used to group all bedtime markers. */
    private var markerSet: MarkerSet? = null

    /** The icon applied to every bed marker. */
    private var markerIcon: MarkerIcon? = null

    /** Convenience accessor for the owning [Bedtime] module. */
    val module: Bedtime?
        get() = parent.module

    /** Registers Dynmap listeners and creates the bedtime marker layer when available. */
    fun onEnable() {
        val m = module ?: return
        if (
            !DynmapIntegration.initialize(
                m.log,
                { d, m ->
                    dynmapApi = d
                    markerApi = m
                },
            ) {
                dynmapEnabled = true
                createOrLoadLayer()
            }
        )
            return
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

    /** Creates or updates the Dynmap marker set and icon. */
    private fun createOrLoadLayer() {
        /* Create or retrieve layer. */
        val markerApi = markerApi ?: return

        markerSet = markerApi.getMarkerSet(BedtimeDynmapLayer.LAYER_ID)
        if (markerSet == null) {
            markerSet =
                markerApi.createMarkerSet(
                    BedtimeDynmapLayer.LAYER_ID,
                    requireNotNull(parent.langLayerLabel).str(),
                    null,
                    false,
                )
        }

        if (markerSet == null) {
            module?.log?.severe("Failed to create dynmap bedtime marker set!")
            return
        }

        /* Update layer attributes. */
        markerSet?.markerSetLabel = requireNotNull(parent.langLayerLabel).str()
        markerSet?.layerPriority = parent.configLayerPriority
        markerSet?.hideByDefault = parent.configLayerHide

        /* Load marker icon. */
        markerIcon = markerApi.getMarkerIcon(parent.configMarkerIcon)
        if (markerIcon == null) {
            module?.log?.severe("Failed to load dynmap bedtime marker icon!")
            return
        }

        /* Initial refresh. */
        updateAllMarkers()
    }

    /** Returns the Dynmap marker ID string for the given [playerId]. */
    private fun idFor(playerId: UUID): String {
        return playerId.toString()
    }

    /** Returns the Dynmap marker ID string for the given [player]. */
    private fun idFor(player: OfflinePlayer): String {
        return idFor(player.uniqueId)
    }

    /** Creates or replaces a player's bedtime marker. */
    fun updateMarker(player: OfflinePlayer): Boolean {
        if (!dynmapEnabled) {
            return false
        }

        val loc = player.respawnLocation ?: return false

        val worldName = loc.world.name
        val markerId = idFor(player)
        val markerLabel = requireNotNull(parent.langMarkerLabel).str(player.name)

        markerSet?.createMarker(
            markerId,
            markerLabel,
            worldName,
            loc.x,
            loc.y,
            loc.z,
            markerIcon,
            false,
        )
        return true
    }

    /** Removes a marker by player UUID. */
    fun removeMarker(playerId: UUID) {
        removeMarker(idFor(playerId))
    }

    /** Removes a marker by marker identifier. */
    fun removeMarker(markerId: String) {
        if (!dynmapEnabled) {
            return
        }

        removeMarker(markerSet?.findMarker(markerId))
    }

    /** Deletes a marker instance if present. */
    fun removeMarker(marker: Marker?) {
        if (!dynmapEnabled || marker == null) {
            return
        }

        marker.deleteMarker()
    }

    /** Synchronizes all markers with offline player data and prunes stale markers. */
    fun updateAllMarkers() {
        if (!dynmapEnabled) {
            return
        }

        /* Update all existing markers. */
        val idSet = mutableSetOf<String>()
        for (player in module?.offlinePlayersWithValidName.orEmpty()) {
            if (updateMarker(player)) {
                idSet.add(idFor(player))
            }
        }

        /* Remove orphaned markers. */
        for (marker in markerSet?.markers.orEmpty()) {
            val id = marker.markerID
            if (id != null && !idSet.contains(id)) {
                removeMarker(marker)
            }
        }
    }
}
