package org.oddlama.vane.regions

import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.Region
import java.util.*

/**
 * Dynmap integration component for rendering region boundaries.
 */
class RegionDynmapLayer(context: Context<Regions?>) : ModuleComponent<Regions?>(
    context.group("Dynmap", "Enable Dynmap integration. Regions will then be shown on a separate Dynmap layer.")
) {
    /**
     * Owning regions module instance.
     */
    private val regions: Regions
        get() = requireNotNull(module)

    /**
     * Dynmap layer ordering priority.
     */
    @ConfigInt(def = 35, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    /**
     * Whether the dynmap layer is hidden by default.
     */
    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    /**
     * Marker fill color (RGB integer).
     */
    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    var configFillColor: Int = 0

    /**
     * Marker fill opacity.
     */
    @ConfigDouble(def = 0.05, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    var configFillOpacity: Double = 0.0

    /**
     * Marker outline width.
     */
    @ConfigInt(def = 2, min = 1, desc = "Area marker line weight.")
    var configLineWeight: Int = 0

    /**
     * Marker outline color (RGB integer).
     */
    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker line color (0xRRGGBB).")
    var configLineColor: Int = 0

    /**
     * Marker outline opacity.
     */
    @ConfigDouble(def = 1.0, min = 0.0, max = 1.0, desc = "Area marker line opacity.")
    var configLineOpacity: Double = 0.0

    /**
     * Localized label for the dynmap layer.
     */
    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    /**
     * Localized marker label format string.
     */
    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    /**
     * Runtime delegate handling dynmap API calls.
     */
    private var delegate: RegionDynmapLayerDelegate? = null

    /**
     * Enables dynmap integration after plugin lookup succeeds.
     */
    fun delayedOnEnable() {
        val plugin: Plugin = regions.server.pluginManager.getPlugin("dynmap") ?: return

        delegate = RegionDynmapLayerDelegate(this)
        delegate?.onEnable(plugin)
    }

    /**
     * Schedules delayed dynmap initialization.
     */
    override fun onEnable() {
        scheduleNextTick { this.delayedOnEnable() }
    }

    /**
     * Disables dynmap integration and clears delegate state.
     */
    override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /**
     * Updates one region marker on dynmap.
     */
    fun updateMarker(region: Region) {
        delegate?.updateMarker(region)
    }

    /**
     * Removes a marker by region id.
     */
    fun removeMarker(regionId: UUID) {
        delegate?.removeMarker(regionId)
    }

    /**
     * Removes a marker by dynmap marker id.
     */
    fun removeMarker(markerId: String?) {
        delegate?.removeMarker(markerId)
    }

    /**
     * Rebuilds all dynmap markers from current region state.
     */
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }

    companion object {
        /**
         * Stable dynmap marker-set id for region overlays.
         */
        const val LAYER_ID: String = "vane_regions.regions"
    }
}
