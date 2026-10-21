package org.oddlama.vane.busybar.common

/**
 * Short names of the permissions deciding what a bridge receives and may request, without the
 * `vane.busybar.` prefix. Bridges see these names in `permissions`, and backends report them to the
 * proxy.
 */
object BridgePermissions {
    const val RECEIVE_PLAYERS = "receive.players"
    const val RECEIVE_BEDTIME = "receive.bedtime"
    const val RECEIVE_PORTALS = "receive.portals"
    const val RECEIVE_ADVANCEMENTS = "receive.advancements"
    const val RECEIVE_SLIMECHUNK = "receive.slimechunk"
    const val RECEIVE_REGIONS = "receive.regions"
    const val RECEIVE_REGION_VISITORS = "receive.region_visitors"
    const val RECEIVE_SERVER = "receive.server"
    const val RECEIVE_AUTOSTOP = "receive.autostop"
    const val ACTION_PRESENCE = "action.presence"
    const val ACTION_AUTOSTOP_ABORT = "action.autostop_abort"

    /** Every permission, in the order bridges receive them. */
    val ALL =
        listOf(
            RECEIVE_PLAYERS,
            RECEIVE_BEDTIME,
            RECEIVE_PORTALS,
            RECEIVE_ADVANCEMENTS,
            RECEIVE_SLIMECHUNK,
            RECEIVE_REGIONS,
            RECEIVE_REGION_VISITORS,
            RECEIVE_SERVER,
            RECEIVE_AUTOSTOP,
            ACTION_PRESENCE,
            ACTION_AUTOSTOP_ABORT,
        )

    /** Granted to everyone by default; the rest default to operators. */
    val EVERYONE =
        setOf(
            RECEIVE_PLAYERS,
            RECEIVE_BEDTIME,
            RECEIVE_PORTALS,
            RECEIVE_ADVANCEMENTS,
            RECEIVE_SLIMECHUNK,
            RECEIVE_REGIONS,
            ACTION_PRESENCE,
        )

    /** The permission a bridge request needs, or null for unknown requests. */
    fun forAction(action: String): String? =
        when (action) {
            "presence.set" -> ACTION_PRESENCE
            "autostop.abort" -> ACTION_AUTOSTOP_ABORT
            else -> null
        }
}
