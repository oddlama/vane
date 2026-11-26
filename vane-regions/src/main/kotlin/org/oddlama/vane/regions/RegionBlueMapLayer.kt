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
 * BlueMap integration component for rendering region boundaries.
 */
class RegionBlueMapLayer(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.group("BlueMap", "Enable BlueMap integration.")) {
    /**
     * Owning regions module instance.
     */
    private val regions: Regions
        get() = requireNotNull(module)

    /**
     * Whether the BlueMap marker set is hidden by default.
     */
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    /**
     * Whether marker depth testing is enabled.
     */
    @ConfigBoolean(
        def = true,
        desc = "Set to false to make the area markers visible through terrain and other objects."
    )
    var configDepthTest: Boolean = false

    /**
     * Marker outline width.
     */
    @ConfigInt(def = 2, min = 1, desc = "Area marker line width.")
    var configLineWidth: Int = 0

    /**
     * Marker fill color (RGB integer).
     */
    @ConfigInt(def = 0xffb422, min = 0, max = 0xffffff, desc = "Area marker fill color (0xRRGGBB).")
    var configFillColor: Int = 0

    /**
     * Marker fill opacity.
     */
    @ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Area marker fill opacity.")
    var configFillOpacity: Double = 0.0

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
     * Localized label for the BlueMap marker set.
     */
    @LangMessage
    var langLayerLabel: TranslatedMessage? = null

    /**
     * Localized marker label format string.
     */
    @LangMessage
    var langMarkerLabel: TranslatedMessage? = null

    /**
     * Runtime delegate handling BlueMap API calls.
     */
    private var delegate: RegionBlueMapLayerDelegate? = null

    /**
     * Enables BlueMap integration after plugin lookup succeeds.
     */
    fun delayedOnEnable() {
        val plugin: Plugin = regions.server.pluginManager.getPlugin("BlueMap") ?: return

        delegate = RegionBlueMapLayerDelegate(this)
        delegate?.onEnable(plugin)
    }

    /**
     * Schedules delayed BlueMap initialization.
     */
    override fun onEnable() {
        scheduleNextTick { this.delayedOnEnable() }
    }

    /**
     * Disables BlueMap integration and clears delegate state.
     */
    override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /**
     * Updates one region marker on BlueMap.
     */
    fun updateMarker(region: Region) {
        delegate?.updateMarker(region)
    }

    /**
     * Removes one region marker from BlueMap.
     */
    fun removeMarker(regionId: UUID) {
        delegate?.removeMarker(regionId)
    }

    /**
     * Rebuilds all BlueMap markers from current region state.
     */
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }
}
