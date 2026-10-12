package org.oddlama.vane.proxycore.busybar

import org.json.JSONObject
import java.util.*

/**
 * Receives what linked vane-busybar backends report. Called from the link's own threads, so
 * implementations must be thread-safe.
 */
interface BackendLinkHandler {
    /**
     * Who should receive an event. [PLAYER] carries the player, [SERVER] means bridges whose owner is
     * on (or was last on) the reporting backend, [ALL] means every bridge on the network.
     */
    enum class Scope { ALL, SERVER, PLAYER }

    /**
     * One event from a backend.
     *
     * @property type event name, for example `bedtime.enter`.
     * @property data payload, already holding `server`.
     * @property scope who receives it.
     * @property player the receiving player when [scope] is [Scope.PLAYER].
     * @property permission short permission name such as `receive.bedtime`, or null for none.
     */
    data class Event(
        val type: String,
        val data: JSONObject,
        val scope: Scope,
        val player: UUID?,
        val permission: String?
    )

    /** Players in focus mode, sent to a backend in `welcome`. */
    fun busyPlayers(): Collection<UUID>

    /** Players with a bridge connected, sent to a backend in `welcome`. */
    fun playersWithBridges(): Collection<UUID>

    /** [link] finished its handshake. */
    fun onConnect(link: BackendLink)

    /** [link] reported [event]. */
    fun onEvent(link: BackendLink, event: Event)

    /** [link] reported the short permission names some players hold there. */
    fun onGrants(link: BackendLink, grants: List<BackendLink.Grants>)

    /**
     * [link] went away; [reason] is `bye: <reason>`, `replaced`, `timeout`, `proxy stopping`,
     * `closed`, or an error.
     */
    fun onDisconnect(link: BackendLink, reason: String)
}
