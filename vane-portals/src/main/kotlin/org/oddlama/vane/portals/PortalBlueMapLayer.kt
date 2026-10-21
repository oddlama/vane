package org.oddlama.vane.portals

import java.util.*
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.portal.Portal

/**
 * Module component that integrates public portal markers with BlueMap.
 *
 * @property configHideByDefault whether the BlueMap marker set is hidden by default.
 * @property langLayerLabel localized label for the marker set.
 * @property langMarkerLabel localized HTML marker label template.
 */
class PortalBlueMapLayer(context: Context<Portals?>) :
    ModuleComponent<Portals?>(
        context.group("BlueMap", "Enable BlueMap integration to show public portals.")
    ) {
    /** Whether the marker set is hidden by default. */
    @JvmField
    @ConfigBoolean(def = false, desc = "If the marker set should be hidden by default.")
    var configHideByDefault: Boolean = false

    /** Localized marker set label. */
    @JvmField @LangMessage var langLayerLabel: TranslatedMessage? = null

    /** Localized marker label template for portal markers. */
    @JvmField @LangMessage var langMarkerLabel: TranslatedMessage? = null

    /** Delegate that performs BlueMap API operations. */
    private var delegate: PortalBlueMapLayerDelegate? = null

    /** Enables BlueMap integration after all plugins have finished startup. */
    fun delayedOnEnable() {
        if (module!!.server.pluginManager.getPlugin("BlueMap") == null) return

        delegate = PortalBlueMapLayerDelegate(this)
        delegate!!.onEnable()
    }

    /** Schedules deferred BlueMap integration setup. */
    public override fun onEnable() {
        scheduleNextTick { delayedOnEnable() }
    }

    /** Tears down BlueMap integration state. */
    public override fun onDisable() {
        delegate?.onDisable()
        delegate = null
    }

    /** Updates or creates the BlueMap marker for [portal]. */
    fun updateMarker(portal: Portal) {
        delegate?.updateMarker(portal)
    }

    /** Removes the BlueMap marker for the given portal id. */
    fun removeMarker(portalId: UUID?) {
        delegate?.removeMarker(portalId)
    }

    /** Rebuilds all portal markers in BlueMap. */
    @Suppress("unused")
    fun updateAllMarkers() {
        delegate?.updateAllMarkers()
    }
}
