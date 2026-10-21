package org.oddlama.vane.bedtime

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.HtmlMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import java.util.*
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.oddlama.vane.util.TextUtil

/**
 * Handles BlueMap API integration and bedtime marker synchronization.
 *
 * @param parent owning BlueMap layer component.
 * @constructor Creates a BlueMap integration delegate bound to its parent layer.
 */
class BedtimeBlueMapLayerDelegate(private val parent: BedtimeBlueMapLayer) {
    /** Whether BlueMap integration is currently active. */
    private var bluemapEnabled = false

    /** Convenience accessor for the owning [Bedtime] module. */
    val module: Bedtime?
        get() = parent.module

    /** Registers BlueMap callback and initializes marker sets once BlueMap is enabled. */
    fun onEnable() {
        BlueMapAPI.onEnable { api: BlueMapAPI? ->
            val api = api ?: return@onEnable
            val module = module ?: return@onEnable

            module.log.info("Enabling BlueMap integration")
            bluemapEnabled = true

            /* Create marker sets. */
            for (world in module.server.worlds) {
                createMarkerSet(api, world)
            }
            updateAllMarkers()
        }
    }

    /** Disables BlueMap integration state. */
    fun onDisable() {
        if (!bluemapEnabled) {
            return
        }

        module?.log?.info("Disabling BlueMap integration")
        bluemapEnabled = false
    }

    /** Marker sets keyed by world UUID. */
    private val markerSets = mutableMapOf<UUID, MarkerSet>()

    /** Creates and attaches a marker set to all BlueMap maps for the given world. */
    private fun createMarkerSet(api: BlueMapAPI, world: World) {
        if (world.uid in markerSets) return

        val markerSet =
            MarkerSet.builder()
                .label(requireNotNull(parent.langLayerLabel).str())
                .toggleable(true)
                .defaultHidden(parent.configHideByDefault)
                .build()

        api.getWorld(world).ifPresent { bmWorld: BlueMapWorld ->
            for (map in bmWorld.maps) {
                map.markerSets[MARKER_SET_ID] = markerSet
            }
        }

        markerSets[world.uid] = markerSet
    }

    /** Creates or replaces a player's bedtime marker across worlds. */
    fun updateMarker(player: OfflinePlayer) {
        removeMarker(player.uniqueId)
        val loc = player.respawnLocation ?: return
        val world = loc.world ?: return

        val marker =
            HtmlMarker.builder()
                .position(loc.x, loc.y, loc.z)
                .label("Bed for ${player.name}")
                .html(requireNotNull(parent.langMarkerLabel).str(TextUtil.escapeHtml(player.name)))
                .build()

        /* Existing markers are overwritten. */
        markerSets[world.uid]?.markers?.set(player.uniqueId.toString(), marker)
    }

    /** Removes a player's marker from all known world marker sets. */
    fun removeMarker(playerId: UUID) {
        for (markerSet in markerSets.values) {
            markerSet.markers.remove(playerId.toString())
        }
    }

    /** Synchronizes markers for all known offline players. */
    fun updateAllMarkers() {
        /* Update all existing markers. */
        for (player in module?.offlinePlayersWithValidName.orEmpty()) {
            updateMarker(player)
        }
    }

    /** Constants for BlueMap bedtime integration. */
    companion object {
        /** Unique BlueMap marker-set identifier for the bedtime layer. */
        const val MARKER_SET_ID: String = "vane_bedtime.bedtime"
    }
}
