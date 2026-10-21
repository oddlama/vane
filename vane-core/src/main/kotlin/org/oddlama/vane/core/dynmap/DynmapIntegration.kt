package org.oddlama.vane.core.dynmap

import java.util.logging.Level
import java.util.logging.Logger
import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import org.dynmap.markers.MarkerAPI

/** Small helper utilities for Dynmap integration used across modules. */
object DynmapIntegration {
    /**
     * Register a DynmapCommonAPIListener and return any immediately-available Dynmap handles.
     * Returns a Pair(dynmapApi, markerApi) when registration succeeded (handles may be null if
     * Dynmap is not present), or null when registration failed (an exception was thrown).
     *
     * Note: DynmapCommonAPIListener may call apiEnabled synchronously when Dynmap is already
     * loaded; this method captures that case and returns the discovered handles. When the API is
     * not present yet the returned pair will contain nulls and the caller may decide how to
     * proceed.
     *
     * @param logger Logger used to emit warnings when registration fails.
     */
    fun tryRegister(logger: Logger): Pair<DynmapCommonAPI?, MarkerAPI?>? {
        var dynmapApi: DynmapCommonAPI? = null
        var markerApi: MarkerAPI? = null

        try {
            DynmapCommonAPIListener.register(
                object : DynmapCommonAPIListener() {
                    override fun apiEnabled(api: DynmapCommonAPI?) {
                        dynmapApi = api
                        markerApi = api?.markerAPI
                    }
                }
            )
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error while enabling dynmap integration!", e)
            return null
        }

        return Pair(dynmapApi, markerApi)
    }

    /**
     * High-level initializer that performs registration, assigns found API handles via [onApis],
     * logs and calls [onStart] to perform module-specific initialization.
     *
     * Returns true when Dynmap integration is available and initialization should proceed.
     */
    fun initialize(
        logger: Logger,
        onApis: (dynmapApi: DynmapCommonAPI?, markerApi: MarkerAPI?) -> Unit,
        onStart: () -> Unit,
    ): Boolean {
        val handles = tryRegister(logger) ?: return false
        onApis(handles.first, handles.second)

        if (handles.second == null) return false

        logger.info("Enabling dynmap integration")
        onStart()
        return true
    }
}
