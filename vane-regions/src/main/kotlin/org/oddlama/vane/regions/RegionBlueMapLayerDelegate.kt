package org.oddlama.vane.regions

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.ExtrudeMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.math.Color
import de.bluecolored.bluemap.api.math.Shape
import java.util.*
import java.util.function.Consumer
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.oddlama.vane.regions.region.Region

/** Low-level BlueMap API delegate used by `RegionBlueMapLayer`. */
class RegionBlueMapLayerDelegate(private val parent: RegionBlueMapLayer) {
    /** Whether BlueMap integration is currently active. */
    private var bluemapEnabled = false

    /** Owning regions module instance. */
    private val module: Regions
        get() = requireNotNull(parent.module)

    /** Registers BlueMap hooks, creates marker sets, and performs initial marker sync. */
    fun onEnable(@Suppress("UNUSED_PARAMETER") plugin: Plugin?) {
        BlueMapAPI.onEnable { api: BlueMapAPI? ->
            val blueMapApi = api ?: return@onEnable
            module.log.info("Enabling BlueMap integration")
            bluemapEnabled = true

            // Create marker sets
            for (world in module.server.worlds) {
                createMarkerSet(blueMapApi, world)
            }
            updateAllMarkers()
        }
    }

    /** Disables BlueMap integration state. */
    fun onDisable() {
        if (!bluemapEnabled) {
            return
        }

        module.log.info("Disabling BlueMap integration")
        bluemapEnabled = false
    }

    // worldId -> marker set
    /** Marker sets keyed by world UUID. */
    private val markerSets = mutableMapOf<UUID, MarkerSet>()

    /** Creates and registers a BlueMap marker set for a world if missing. */
    private fun createMarkerSet(api: BlueMapAPI, world: World) {
        if (markerSets.containsKey(world.uid)) {
            return
        }

        val markerSet =
            MarkerSet.builder()
                .label(parent.langLayerLabel?.str() ?: "")
                .toggleable(true)
                .defaultHidden(parent.configHideByDefault)
                .build()

        api.getWorld(world)
            .ifPresent(
                Consumer { bmWorld: BlueMapWorld? ->
                    for (map in bmWorld?.maps.orEmpty()) {
                        map.markerSets[MARKER_SET_ID] = markerSet
                    }
                }
            )

        markerSets[world.uid] = markerSet
    }

    /** Creates or replaces the marker for one region. */
    fun updateMarker(region: Region) {
        val regionId = region.id() ?: return
        val extent = region.extent() ?: return
        val min = extent.min() ?: return
        val max = extent.max() ?: return
        removeMarker(regionId)
        val shape =
            Shape.createRect(
                min.x.toDouble(),
                min.z.toDouble(),
                (max.x + 1).toDouble(),
                (max.z + 1).toDouble(),
            )

        val marker =
            ExtrudeMarker.builder()
                .shape(shape, min.y.toFloat(), (max.y + 1).toFloat())
                .label(parent.langMarkerLabel?.str(region.name()) ?: region.name())
                .lineWidth(parent.configLineWidth)
                .lineColor(Color(parent.configLineColor, parent.configLineOpacity.toFloat()))
                .fillColor(Color(parent.configFillColor, parent.configFillOpacity.toFloat()))
                .depthTestEnabled(parent.configDepthTest)
                .centerPosition()
                .build()

        // Existing markers will be overwritten.
        markerSets[min.world.uid]?.markers?.set(regionId.toString(), marker)
    }

    /** Removes a region marker from every world marker set. */
    fun removeMarker(regionId: UUID) {
        for (markerSet in markerSets.values) {
            markerSet.markers.remove(regionId.toString())
        }
    }

    /** Synchronizes all BlueMap markers for loaded regions. */
    fun updateAllMarkers() {
        for (region in module.allRegions()) {
            val r = region ?: continue
            updateMarker(r)
        }
    }

    companion object {
        /** Stable marker-set id used across all BlueMap worlds. */
        const val MARKER_SET_ID: String = "vane_regions.regions"
    }
}
