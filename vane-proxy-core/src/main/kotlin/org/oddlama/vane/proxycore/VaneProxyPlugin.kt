package org.oddlama.vane.proxycore

import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import org.oddlama.vane.proxycore.busybar.BusyBarProxy
import org.oddlama.vane.proxycore.config.ConfigManager
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.config.ManagedServer
import org.oddlama.vane.proxycore.config.ManagedServer.ConfigItemSource
import org.oddlama.vane.proxycore.listeners.PreLoginEvent.MultiplexedPlayer
import org.oddlama.vane.proxycore.log.IVaneLogger

/** Base plugin abstraction shared by proxy platform implementations. */
abstract class VaneProxyPlugin {
    /** Parsed configuration manager for proxy-core features. */
    @JvmField var config: ConfigManager = ConfigManager(this)

    /** Maintenance state controller. */
    @JvmField var maintenance: Maintenance = Maintenance(this)

    /** vane-busybar backend links. */
    @JvmField var busyBar: BusyBarProxy = BusyBarProxy(this)

    /** Logger adapter bound by the concrete proxy implementation. */
    @JvmField var logger: IVaneLogger? = null

    /** Runtime proxy server abstraction. */
    @JvmField var server: ProxyServer? = null

    /** Data folder used for configuration and persistence. */
    @JvmField var dataFolder: File? = null

    /** Mapping of generated multiplexed UUIDs back to original UUIDs. */
    val multiplexedUuids: LinkedHashMap<UUID?, UUID?> = LinkedHashMap()

    /** Multiplexed players awaiting backend registration. */
    @JvmField
    val pendingMultiplexerLogins: LinkedHashMap<UUID?, MultiplexedPlayer?> = LinkedHashMap()

    /** Guard that prevents overlapping start command attempts. */
    private var serverStarting = false

    /**
     * Checks whether a backend server is reachable by opening a short TCP connection.
     *
     * @param server server descriptor to probe.
     * @return `true` when a connection can be established.
     */
    fun isOnline(server: IVaneProxyServerInfo): Boolean {
        val addr = server.socketAddress
        if (addr !is InetSocketAddress) {
            return false
        }

        var connected = false
        try {
            Socket(addr.hostName, addr.port).use { test ->
                connected = test.isConnected
            }
        } catch (e: IOException) {
            // Server not up or not reachable
        }

        return connected
    }

    /**
     * Builds the visible MOTD for [server], considering maintenance mode and online state.
     *
     * @param server target backend server.
     * @return formatted MOTD string.
     */
    fun getMotd(server: IVaneProxyServerInfo): String {
        // Maintenance
        if (maintenance.enabled()) {
            return maintenance.formatMessage(Maintenance.MOTD)
        }

        val cms = config.managedServers[server.name] ?: return ""
        val source = if (isOnline(server)) ConfigItemSource.ONLINE else ConfigItemSource.OFFLINE

        return cms.motd(source)
    }

    /**
     * Resolves the encoded favicon for [server] according to current online state.
     *
     * @param server target backend server.
     * @return base64 data URL favicon or `null` when not configured.
     */
    fun getFavicon(server: IVaneProxyServerInfo): String? {
        val cms = config.managedServers[server.name] ?: return null
        val source = if (isOnline(server)) ConfigItemSource.ONLINE else ConfigItemSource.OFFLINE

        return cms.favicon(source)
    }

    /** Proxy runtime, guaranteed to be initialized. */
    val proxy: ProxyServer
        get() = checkNotNull(server) { "Proxy server is not initialized" }

    /**
     * Returns the configured logger.
     *
     * @return initialized logger instance.
     */
    fun getLogger(): IVaneLogger = checkNotNull(logger) { "Logger is not initialized" }

    /**
     * Attempts to start a managed backend server asynchronously.
     *
     * @param server managed server to start.
     */
    fun tryStartServer(server: ManagedServer) {
        // FIXME: this is not async-safe and there might be conditions where two start commands can
        // be executed
        // simultaneously. Don't rely on this as a user - instead use a start command that is
        // atomic.
        if (serverStarting) return

        // Ensure we have a scheduler to run the start task
        val scheduler = this.server?.scheduler
        if (scheduler == null) {
            getLogger().log(Level.SEVERE, "No scheduler available to start server '${server.id()}'")
            return
        }

        // Resolve and validate the start command before using the spread operator
        val rawCmd: Array<String?>? = server.startCmd()
        val cmd: Array<String>
        if (rawCmd == null) {
            getLogger()
                .log(Level.SEVERE, "Start command for server '${server.id()}' is not configured.")
            return
        } else {
            // Filter out any null elements; if after filtering there are no args, abort
            val filtered = rawCmd.filterNotNull()
            if (filtered.isEmpty()) {
                getLogger()
                    .log(
                        Level.SEVERE,
                        "Start command for server '${server.id()}' is empty after filtering nulls.",
                    )
                return
            }
            cmd = filtered.toTypedArray()
        }

        scheduler.runAsync(this) {
            try {
                serverStarting = true
                getLogger()
                    .log(
                        Level.INFO,
                        "Running start command for server '${server.id()}': ${cmd.contentToString()}",
                    )
                val processBuilder = ProcessBuilder(*cmd)
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                val process = processBuilder.start()

                val commandTimeout = server.commandTimeout()?.toLong() ?: 10L
                if (!process.waitFor(commandTimeout, TimeUnit.SECONDS)) {
                    getLogger()
                        .log(Level.SEVERE, "Server '${server.id()}'s start command timed out!")
                }

                if (process.exitValue() != 0) {
                    getLogger()
                        .log(
                            Level.SEVERE,
                            "Server '${server.id()}'s start command returned a nonzero exit code!",
                        )
                }
            } catch (e: Exception) {
                getLogger().log(Level.SEVERE, "Failed to start server '${server.id()}'", e)
            }
            serverStarting = false
        }
    }

    /**
     * Determines whether [uuid] is allowed to join while maintenance mode is active.
     *
     * @param uuid player UUID to test.
     * @return `true` if the player can join.
     */
    fun canJoinMaintenance(uuid: UUID?): Boolean =
        !maintenance.enabled() || proxy.hasPermission(uuid, "vane_proxy.bypass_maintenance")

    companion object {
        /** Namespace part of the auth multiplex plugin message channel. */
        const val CHANNEL_AUTH_MULTIPLEX_NAMESPACE: String = "vane_proxy"

        /** Name part of the auth multiplex plugin message channel. */
        const val CHANNEL_AUTH_MULTIPLEX_NAME: String = "auth_multiplex"

        /** Full auth multiplex plugin message channel identifier. */
        const val CHANNEL_AUTH_MULTIPLEX: String =
            "$CHANNEL_AUTH_MULTIPLEX_NAMESPACE:$CHANNEL_AUTH_MULTIPLEX_NAME"
    }
}
