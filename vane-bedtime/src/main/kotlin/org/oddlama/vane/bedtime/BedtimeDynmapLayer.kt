package org.oddlama.vane.bedtime

import java.util.*
import org.bukkit.OfflinePlayer
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Configurable Dynmap layer for bedtime spawn markers.
 *
 * @param context component context used to register config and lifecycle hooks.
 * @constructor Creates the Dynmap layer component.
 */
class BedtimeDynmapLayer(context: Context<Bedtime?>) :
    ModuleComponent<Bedtime?>(
        context.group(
            "Dynmap",
            "Enable Dynmap integration. Player spawnpoints (beds) will then be shown on a separate dynmap layer.",
        )
    ) {
    /** Z-order priority used when stacking Dynmap layers. */
    @ConfigInt(def = 25, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    /** Whether the layer starts hidden in the Dynmap UI. */
    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    /** Identifier of the Dynmap built-in icon to use for bed markers. */
    @ConfigString(def = "house", desc = "The dynmap marker icon.")
    var configMarkerIcon: String? = null

    /** Display label shown for this layer in the Dynmap UI. */
    @LangMessage var langLayerLabel: TranslatedMessage? = null

    /** Label rendered inside each individual bed marker popup. */
    @LangMessage var langMarkerLabel: TranslatedMessage? = null

    /** Active delegate; `null` when Dynmap is unavailable. */
    private var delegate: BedtimeDynmapLayerDelegate? = null

    /** Initializes Dynmap integration after plugin load. */
    fun delayedOnEnable() {
        module?.server?.pluginManager?.getPlugin("dynmap") ?: return

        delegate = BedtimeDynmapLayerDelegate(this)
        delegate?.onEnable()
    }

    /** Triggers delayed initialization one tick after module enable. */
    public override fun onEnable() {
        scheduleNextTick(::delayedOnEnable)
    }

    /** Shuts down Dynmap integration delegate. */
    public override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /** Updates the marker for one player if the delegate is active. */
    fun updateMarker(player: OfflinePlayer?) {
        player?.let { delegate?.updateMarker(it) }
    }

    /** Removes a marker by player UUID. */
    fun removeMarker(playerId: UUID?) {
        playerId?.let { delegate?.removeMarker(it) }
    }

    /** Removes a marker by marker identifier. */
    fun removeMarker(markerId: String?) {
        markerId?.let { delegate?.removeMarker(it) }
    }

    /** Rebuilds all bedtime markers in Dynmap. */
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }

    /** Constants for Dynmap bedtime integration. */
    companion object {
        /** Unique Dynmap marker-set identifier for the bedtime layer. */
        const val LAYER_ID: String = "vane_bedtime.bedtime"
    }
}
