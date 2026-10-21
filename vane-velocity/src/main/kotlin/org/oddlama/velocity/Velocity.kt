package org.oddlama.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.proxy.VelocityServer
import com.velocitypowered.proxy.network.ConnectionManager
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.function.BiConsumer
import java.util.logging.Level
import org.bstats.velocity.Metrics
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.log.Slf4jCompatLogger
import org.oddlama.vane.proxycore.util.VERSION
import org.oddlama.velocity.commands.BusyBar
import org.oddlama.velocity.commands.Maintenance
import org.oddlama.velocity.commands.Ping
import org.oddlama.velocity.compat.VelocityCompatProxyServer
import org.oddlama.velocity.listeners.*
import org.slf4j.Logger

@Plugin(
    id = "vane-velocity",
    name = "Vane Velocity",
    version = VERSION,
    description = "TODO",
    authors = ["oddlama", "Serial-ATA"],
    url = "https://github.com/oddlama/vane",
)
/**
 * Velocity platform entrypoint for the Vane proxy plugin.
 *
 * @param server injected Velocity proxy server instance.
 * @param logger injected platform logger.
 * @param metricsFactory bStats factory used to initialize metrics reporting.
 * @param dataDir plugin data directory provided by Velocity.
 */
class Velocity
@Inject
constructor(
    server: ProxyServer,
    logger: Logger,
    metricsFactory: Metrics.Factory,
    @DataDirectory dataDir: Path,
) : VaneProxyPlugin() {
    /**
     * Raw Velocity proxy server used for API integrations that are not abstracted by proxy-core.
     */
    val rawProxy: ProxyServer

    // bStats
    @Suppress("unused")
    /** bStats factory retained so metrics can be initialized during plugin startup. */
    private val metricsFactory: Metrics.Factory

    init {
        this.server = VelocityCompatProxyServer(server)
        this.logger = Slf4jCompatLogger(logger)

        this.metricsFactory = metricsFactory

        this.rawProxy = server
        this.dataFolder = dataDir.toFile()
    }

    /**
     * Handles Velocity initialization and registers listeners, commands, and channels.
     *
     * @param event proxy initialization event.
     */
    @Subscribe
    fun onEnable(@Suppress("UNUSED_PARAMETER") event: ProxyInitializeEvent) {
        if (!config.load()) {
            disable()
            return
        }

        metricsFactory.make(this, 8891)

        val eventManager = rawProxy.eventManager

        eventManager.register(this, ProxyPingListener(this))
        eventManager.register(this, ProxyPreLoginListener(this))
        eventManager.register(this, ProxyGameProfileRequestListener(this))
        eventManager.register(this, ProxyLoginListener(this))
        eventManager.register(this, ProxyDisconnectListener(this))
        eventManager.register(this, ProxyBusyBarListener(this))

        maintenance.load()
        busyBar.start()

        val commandManager = rawProxy.commandManager

        val pingMeta = commandManager.metaBuilder("ping").build()
        commandManager.register(pingMeta, Ping(this))

        val maintenanceMeta = commandManager.metaBuilder("maintenance").build()
        commandManager.register(maintenanceMeta, Maintenance(this))

        // Only while serving BUSY Bars: otherwise backends may serve them, and a proxy command
        // would hide theirs from players.
        if (busyBar.running) {
            commandManager.register(commandManager.metaBuilder("busybar").build(), BusyBar(this))
        }

        rawProxy.channelRegistrar.register(CHANNEL)

        if (config.multiplexerById.isNotEmpty()) {
            try {
                getLogger().log(Level.INFO, "Attempting to register auth multiplexers")

                // Velocity doesn't let you register multiple listeners like Bungeecord,
                // So we have to take matters into our own hands :)
                handleListeners(
                    "Registering",
                    BiConsumer { connectionManager, address -> connectionManager.bind(address) },
                )
            } catch (e: Exception) {
                getLogger().log(Level.SEVERE, "Failed to inject into VelocityServer!", e)
                disable()
            }
        }
    }

    /**
     * Handles Velocity shutdown and delegates to plugin cleanup.
     *
     * @param event proxy shutdown event.
     */
    @Subscribe
    fun onDisable(@Suppress("UNUSED_PARAMETER") event: ProxyShutdownEvent) {
        disable()
    }

    /** Cleans up listeners, channels, and injected multiplexer listeners. */
    private fun disable() {
        rawProxy.eventManager.unregisterListeners(this)
        busyBar.stop()

        rawProxy.channelRegistrar.unregister(CHANNEL)

        // Now let's be good and clean up our mess :)
        try {
            getLogger().log(Level.INFO, "Attempting to close auth multiplexers")

            handleListeners(
                "Closing",
                BiConsumer { connectionManager, oldBind -> connectionManager.close(oldBind) },
            )
        } catch (e: Exception) {
            getLogger().log(Level.SEVERE, "Failed to stop listeners!", e)
            getLogger()
                .log(
                    Level.SEVERE,
                    "Shutting down the server to prevent lingering unmanaged listeners!",
                )
            rawProxy.shutdown()
        }

        server = null
        logger = null
    }

    /**
     * Applies an action to each configured auth multiplexer listener address.
     *
     * @param action human-readable action label used in logs.
     * @param method operation to apply to the internal Velocity connection manager.
     * @throws ClassNotFoundException when Velocity internals cannot be loaded reflectively.
     * @throws NoSuchFieldException when the internal connection manager field changes.
     * @throws IllegalAccessException when reflective access to Velocity internals fails.
     */
    @Throws(
        ClassNotFoundException::class,
        NoSuchFieldException::class,
        IllegalAccessException::class,
    )
    private fun handleListeners(
        action: String,
        method: BiConsumer<in ConnectionManager, in InetSocketAddress>,
    ) {
        val server =
            rawProxy as? VelocityServer
                ?: throw IllegalStateException("ProxyServer is not a VelocityServer implementation")

        // We steal the VelocityServer's `ConnectionManager`, which (currently) has no
        // issue binding to however many addresses we give it.
        val velocityServer = Class.forName("com.velocitypowered.proxy.VelocityServer")
        val cmField = velocityServer.getDeclaredField("cm")
        cmField.isAccessible = true

        val connectionManager = cmField.get(server) as ConnectionManager

        for ((id, multiplexerConfig) in config.multiplexerById) {
            val port = multiplexerConfig?.port ?: continue

            getLogger().log(Level.INFO, "$action multiplexer ID $id, bound to port $port")

            val address = InetSocketAddress(port)
            method.accept(connectionManager, address)
        }
    }

    /** Static plugin constants. */
    companion object {
        @JvmField
        /** Plugin messaging channel used by auth multiplexing. */
        val CHANNEL: MinecraftChannelIdentifier =
            MinecraftChannelIdentifier.create(
                CHANNEL_AUTH_MULTIPLEX_NAMESPACE,
                CHANNEL_AUTH_MULTIPLEX_NAME,
            )
    }
}
