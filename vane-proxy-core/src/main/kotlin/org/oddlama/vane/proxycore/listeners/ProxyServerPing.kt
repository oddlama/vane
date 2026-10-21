package org.oddlama.vane.proxycore.listeners

/** Mutable ping response abstraction. */
interface ProxyServerPing {
    /**
     * Sets the server list ping description text.
     *
     * @param description MOTD to return to clients.
     */
    fun setDescription(description: String?)

    /**
     * Sets the encoded favicon shown in the server list.
     *
     * @param encodedFavicon favicon data URL.
     */
    fun setFavicon(encodedFavicon: String?)
}
