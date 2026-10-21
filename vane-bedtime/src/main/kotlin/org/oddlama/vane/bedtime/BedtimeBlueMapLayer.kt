package org.oddlama.vane.bedtime

import java.util.*
import org.bukkit.OfflinePlayer
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Configurable BlueMap layer for bedtime spawn markers.
 *
 * @param context component context used to register config and lifecycle hooks.
 * @constructor Creates the BlueMap layer component.
 */
class BedtimeBlueMapLayer(context: Context<Bedtime?>) :
    ModuleComponent<Bedtime?>(
        context.group(
            "BlueMap",
            "Enable BlueMap integration to show player spawnpoints (beds).",
        )
    ) {
    /** Whether the marker set starts hidden in the BlueMap UI. */
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    /** Display label shown for this layer in the BlueMap UI. */
    @LangMessage var langLayerLabel: TranslatedMessage? = null

    /** Label rendered inside each individual bed marker popup. */
    @LangMessage var langMarkerLabel: TranslatedMessage? = null

    /** Active delegate; `null` when BlueMap is unavailable. */
    private var delegate: BedtimeBlueMapLayerDelegate? = null

    /** Initializes BlueMap integration after plugin load. */
    fun delayedOnEnable() {
        module?.server?.pluginManager?.getPlugin("BlueMap") ?: return

        delegate = BedtimeBlueMapLayerDelegate(this)
        delegate?.onEnable()
    }

    /** Triggers delayed initialization one tick after module enable. */
    public override fun onEnable() {
        scheduleNextTick(::delayedOnEnable)
    }

    /** Shuts down BlueMap integration delegate. */
    public override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /** Updates the marker for one player if the delegate is active. */
    fun updateMarker(player: OfflinePlayer) {
        delegate?.updateMarker(player)
    }

    /** Removes a marker by player UUID. */
    fun removeMarker(playerId: UUID?) {
        playerId?.let { delegate?.removeMarker(it) }
    }

    /** Rebuilds all bedtime markers in BlueMap. */
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }
}
