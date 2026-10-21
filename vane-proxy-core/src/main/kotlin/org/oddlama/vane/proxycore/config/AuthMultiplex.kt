package org.oddlama.vane.proxycore.config

import java.util.*

/**
 * Configuration for an authentication multiplexer endpoint.
 *
 * @property port external port bound to this multiplexer.
 * @constructor Creates a multiplexer configuration from configured UUID allowlist strings.
 */
class AuthMultiplex(@JvmField var port: Int?, allowedUuids: MutableList<String?>?) {
    /** Parsed allowlist UUIDs. Empty means unrestricted access. */
    private val allowedUuids: MutableList<UUID?> =
        allowedUuids
            ?.asSequence()
            ?.filterNotNull()
            ?.filter { it.isNotEmpty() }
            ?.map { UUID.fromString(it) }
            ?.toMutableList() ?: mutableListOf()

    /**
     * Checks whether [uuid] is allowed to use this multiplexer.
     *
     * @param uuid UUID to validate.
     * @return `true` when unrestricted or present in the allowlist.
     */
    fun uuidIsAllowed(uuid: UUID?): Boolean = allowedUuids.isEmpty() || uuid in allowedUuids
}
