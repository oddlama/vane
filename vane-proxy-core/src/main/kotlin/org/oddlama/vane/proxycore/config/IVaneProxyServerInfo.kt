package org.oddlama.vane.proxycore.config

import java.net.SocketAddress

/** Describes a backend server target known by the proxy implementation. */
interface IVaneProxyServerInfo {
    /** Backend server identifier used in configuration lookups. */
    val name: String?

    /** Socket address used to probe backend online state. */
    val socketAddress: SocketAddress?

    /**
     * Sends plugin message payload data to the backend.
     *
     * @param data encoded payload.
     */
    fun sendData(data: ByteArray?)

    /**
     * Sends plugin message payload data to the backend.
     *
     * @param data encoded payload.
     * @param queue whether to queue when direct send is unavailable.
     * @return `true` when accepted for delivery.
     */
    fun sendData(data: ByteArray?, queue: Boolean): Boolean
}
