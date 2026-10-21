package org.oddlama.vane.proxycore.listeners

import org.oddlama.vane.proxycore.ProxyPendingConnection

/** Base contract for proxy event wrappers. */
interface ProxyEvent {
    /** Connection associated with this event. */
    val connection: ProxyPendingConnection?

    /** Executes event logic. */
    fun fire()
}
