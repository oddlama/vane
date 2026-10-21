package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig

/**
 * BUSY Bar settings from the `[BusyBar]` table: the stream bridges connect to, and the link
 * vane-busybar backends report to (`[BusyBar.Link]`).
 *
 * @property enabled whether the proxy serves BUSY Bars at all.
 * @property bind address the bridge stream binds to.
 * @property port port of the bridge stream.
 * @property publicUrl address bridges reach the stream at, put into pairing strings.
 * @property tls `auto`, `keystore` or `off`, as in vane-busybar.
 * @property tlsKeystore PKCS#12 keystore for `keystore`, relative to the proxy directory.
 * @property tlsKeystorePassword password of [tlsKeystore].
 * @property allowedActions requests bridges may send.
 * @property linkBind address backends connect to.
 * @property linkPort port backends connect to.
 * @property linkSecret shared with each backend's `ProxyLinkSecret`.
 */
data class BusyBarConfig(
    val enabled: Boolean,
    val bind: String,
    val port: Int,
    val publicUrl: String,
    val tls: String,
    val tlsKeystore: String,
    val tlsKeystorePassword: String,
    val allowedActions: List<String>,
    val linkBind: String,
    val linkPort: Int,
    val linkSecret: String,
) {
    companion object {
        /** Reads the table, using defaults for missing keys. Null when [config] is null. */
        fun from(config: CommentedConfig?): BusyBarConfig? {
            config ?: return null
            val link = config.get<CommentedConfig>("Link")
            return BusyBarConfig(
                enabled = config.getOrElse("Enabled", false),
                bind = config.getOrElse("Bind", "0.0.0.0"),
                port = port(config, "Port", 9123),
                publicUrl = config.getOrElse("PublicUrl", ""),
                tls = config.getOrElse("Tls", "auto"),
                tlsKeystore = config.getOrElse("TlsKeystore", ""),
                tlsKeystorePassword = config.getOrElse("TlsKeystorePassword", ""),
                allowedActions =
                    config
                        .getOrElse<List<Any?>>(
                            "AllowedActions",
                            listOf("presence.set", "autostop.abort"),
                        )
                        .mapNotNull { it?.toString() },
                linkBind = link?.getOrElse("Bind", "127.0.0.1") ?: "127.0.0.1",
                linkPort = link?.let { port(it, "Port", 9124) } ?: 9124,
                linkSecret = link?.getOrElse("Secret", "") ?: "",
            )
        }

        private fun port(config: CommentedConfig, key: String, default: Int): Int =
            config.getIntOrElse(key, default).also {
                require(it in 1..65535) { "BusyBar $key must be between 1 and 65535" }
            }
    }
}
