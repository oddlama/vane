package org.oddlama.vane.portals

import java.util.*
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.portal.Portal

/**
 * Module component that integrates public portal markers with Dynmap.
 *
 * @property configLayerPriority display priority of the Dynmap layer.
 * @property configLayerHide whether the layer is hidden by default.
 * @property configMarkerIcon Dynmap icon id for portal markers.
 * @property langLayerLabel localized layer label.
 * @property langMarkerLabel localized marker label template.
 */
class PortalDynmapLayer(context: Context<Portals?>) :
    ModuleComponent<Portals?>(
        context.group(
            "Dynmap",
            "Enable Dynmap integration. Public portals will then be shown on a separate Dynmap layer.",
        )
    ) {
    /** Display priority for the Dynmap layer. */
    @JvmField
    @ConfigInt(def = 29, min = 0, desc = "Layer ordering priority.")
    var configLayerPriority: Int = 0

    /** Whether the Dynmap layer is hidden by default. */
    @JvmField
    @ConfigBoolean(def = false, desc = "If the layer should be hidden by default.")
    var configLayerHide: Boolean = false

    /** Dynmap marker icon id for portal markers. */
    @JvmField
    @ConfigString(def = "compass", desc = "The Dynmap marker icon.")
    var configMarkerIcon: String? = null

    /** Localized Dynmap layer label. */
    @JvmField @LangMessage var langLayerLabel: TranslatedMessage? = null

    /** Localized marker label template. */
    @JvmField @LangMessage var langMarkerLabel: TranslatedMessage? = null

    /** Delegate that performs Dynmap API operations. */
    private var delegate: PortalDynmapLayerDelegate? = null

    /** Enables Dynmap integration after plugin startup has settled. */
    fun delayedOnEnable() {
        if (module!!.server.pluginManager.getPlugin("Dynmap") == null) return

        delegate = PortalDynmapLayerDelegate(this)
        delegate!!.onEnable()
    }

    /** Schedules deferred Dynmap integration setup. */
    public override fun onEnable() {
        scheduleNextTick { delayedOnEnable() }
    }

    /** Tears down Dynmap integration state. */
    public override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /** Updates or creates the Dynmap marker for [portal]. */
    fun updateMarker(portal: Portal?) {
        portal?.let { delegate?.updateMarker(it) }
    }

    /** Removes the Dynmap marker for the given portal id. */
    fun removeMarker(portalId: UUID?) {
        portalId?.let { delegate?.removeMarker(it) }
    }

    /** Rebuilds all portal markers in Dynmap. */
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }

    /** Shared constants for Dynmap integration. */
    companion object {
        /** Marker set id used for the portal layer. */
        const val LAYER_ID: String = "vane_portals.portals"
    }
}
