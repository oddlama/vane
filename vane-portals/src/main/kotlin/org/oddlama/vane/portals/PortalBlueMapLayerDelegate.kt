package org.oddlama.vane.portals

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.HtmlMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import java.util.*
import java.util.function.Consumer
import org.bukkit.World
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.util.TextUtil

/** Internal BlueMap integration helper used by [PortalBlueMapLayer]. */
class PortalBlueMapLayerDelegate(private val parent: PortalBlueMapLayer) {
    /** Tracks whether BlueMap integration is currently active. */
    private var bluemapEnabled = false

    /** Returns the owning portals module. */
    val module: Portals?
        get() = parent.module

    /** Registers BlueMap enable hook and initializes markers when available. */
    fun onEnable() {
        BlueMapAPI.onEnable enabled@{ api: BlueMapAPI? ->
            val module = module ?: return@enabled
            val enabledApi = api ?: return@enabled

            module.log.info("Enabling BlueMap integration")
            bluemapEnabled = true

            // Create marker sets
            module.server.worlds.forEach { world -> createMarkerSet(enabledApi, world) }
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

    /** Marker sets indexed by Bukkit world id. */
    private val markerSets = HashMap<UUID?, MarkerSet>()

    /** Creates and registers a BlueMap marker set for [world] if missing. */
    private fun createMarkerSet(api: BlueMapAPI, world: World) {
        if (markerSets.containsKey(world.uid)) {
            return
        }

        val markerSet =
            MarkerSet.builder()
                .label(parent.langLayerLabel!!.str())
                .toggleable(true)
                .defaultHidden(parent.configHideByDefault)
                .build()

        api.getWorld(world)
            .ifPresent(
                Consumer { bmWorld: BlueMapWorld? ->
                    bmWorld?.maps?.forEach { map ->
                        map.markerSets[MARKER_SET_ID] = markerSet
                    }
                }
            )

        markerSets[world.uid] = markerSet
    }

    /** Creates or refreshes the BlueMap marker for [portal]. */
    fun updateMarker(portal: Portal) {
        removeMarker(portal.id())

        // Don't show private portals
        if (portal.visibility() == Portal.Visibility.PRIVATE) {
            return
        }

        val loc = portal.spawn()
        val marker =
            HtmlMarker.builder()
                .position(loc.x, loc.y, loc.z)
                .label("Portal " + portal.name())
                .html(parent.langMarkerLabel!!.str(TextUtil.escapeHtml(portal.name())))
                .build()

        // Existing markers will be overwritten.
        val pid = portal.id()?.toString() ?: return
        markerSets[loc.world.uid]!!.markers[pid] = marker
    }

    /** Removes the marker belonging to [portalId] from all worlds. */
    fun removeMarker(portalId: UUID?) {
        markerSets.values.forEach { markerSet ->
            markerSet.markers.remove(portalId?.toString())
        }
    }

    /** Rebuilds all BlueMap markers for visible portals. */
    fun updateAllMarkers() {
        module?.allAvailablePortals()?.filterNotNull()?.forEach { portal ->
            // Don't show private portals
            if (portal.visibility() == Portal.Visibility.PRIVATE) {
                return@forEach
            }

            updateMarker(portal)
        }
    }

    /** Constants used by BlueMap marker creation. */
    companion object {
        /** BlueMap marker set id used for portal markers. */
        const val MARKER_SET_ID: String = "vane_portals.portals"
    }
}
