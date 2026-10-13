package org.oddlama.vane.busybar

import org.oddlama.vane.busybar.common.BridgeTls
import java.io.IOException
import java.security.GeneralSecurityException
import javax.net.ssl.SSLContext

/**
 * TLS for the event stream, in the mode set by [BusyBar.configTls]; see [BridgeTls] for the modes.
 *
 * @param busybar owning module.
 */
class Tls(private val busybar: BusyBar) {
    /** Certificate and key, created once the data folder is known. */
    private val identity by lazy { BridgeTls(busybar.dataFolder, busybar.bridgeLog) }

    /** Mode the event stream was last started with. */
    val mode: BridgeTls.Mode
        get() = identity.mode

    /** Base64url SHA-256 of the auto certificate's public key while in auto mode, otherwise null. */
    val pin: String?
        get() = identity.pin

    /** Mode named in the configuration, falling back to auto for unknown values. */
    fun configuredMode(): BridgeTls.Mode = BridgeTls.parseMode(busybar.configTls) ?: BridgeTls.Mode.AUTO

    /** Logs configurations that leave bridges unprotected or unable to connect. Call when enabling. */
    fun checkConfiguration() {
        if (BridgeTls.parseMode(busybar.configTls) == null) {
            busybar.log.warning("Unknown Tls mode '${busybar.configTls}', using auto. Valid modes: auto, keystore, off.")
        }
        if (!busybar.server.serverConfig.isProxyOnlineMode) {
            busybar.log.warning(
                "The server runs in offline mode, so Minecraft traffic is unencrypted. Pairing strings from " +
                    "/busybar link, which hold the token and the key fingerprint, can be read or altered on the way."
            )
        }
    }

    /**
     * Builds the TLS context for the configured mode.
     *
     * @return the context, or null to serve plain HTTP.
     * @throws IOException if a key or keystore file cannot be read or written.
     * @throws GeneralSecurityException if a key, certificate, or keystore is invalid.
     */
    fun load(): SSLContext? = identity.load(configuredMode(), busybar.configTlsKeystore, busybar.configTlsKeystorePassword)

    /** Deletes the auto certificate, so the next [load] generates a new key. */
    fun deleteAutoIdentity() = identity.deleteAutoIdentity()
}
