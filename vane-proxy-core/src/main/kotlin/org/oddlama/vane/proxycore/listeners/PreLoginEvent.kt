package org.oddlama.vane.proxycore.listeners

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import org.oddlama.vane.proxycore.Maintenance
import org.oddlama.vane.proxycore.Util
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.AuthMultiplex
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.util.Resolve.resolveUuid

/**
 * Handles pre-login authentication multiplexing for configured ports.
 *
 * @property plugin owning plugin instance.
 */
abstract class PreLoginEvent(@JvmField var plugin: VaneProxyPlugin) :
    ProxyEvent, ProxyCancellableEvent {
    /** Unused default fire entry point. Use [fire] with [PreLoginDestination] instead. */
    override fun fire() {
        // Not applicable
        assert(false)
    }

    /**
     * Executes multiplex authentication flow and stores resolved multiplex data in [destination].
     *
     * @param destination target storage for multiplexed player mappings.
     */
    fun fire(destination: PreLoginDestination) {
        val connection = connection ?: return

        // Multiplex authentication if the connection is to a multiplexing port
        val port = connection.port
        val multiplexer: MutableMap.MutableEntry<Int?, AuthMultiplex?> =
            plugin.config.getMultiplexerForPort(port) ?: return

        val multiplexerId = multiplexer.key ?: return

        // This is pre-authentication, so we need to resolve the uuid ourselves.
        val playerName = connection.name ?: return
        val uuid: UUID

        try {
            uuid = resolveUuid(playerName)
        } catch (e: IOException) {
            plugin
                .getLogger()
                .log(Level.WARNING, "Failed to resolve UUID for player '$playerName'", e)
            return
        } catch (e: URISyntaxException) {
            plugin
                .getLogger()
                .log(Level.WARNING, "Failed to resolve UUID for player '$playerName'", e)
            return
        }

        if (!plugin.canJoinMaintenance(uuid)) {
            this.cancel(plugin.maintenance.formatMessage(Maintenance.MESSAGE_CONNECT))
            return
        }

        val authMultiplex = multiplexer.value ?: return
        if (!authMultiplex.uuidIsAllowed(uuid)) {
            this.cancel(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK)
            return
        }

        val name = connection.name ?: return
        val newUuid: UUID = Util.addUuid(uuid, multiplexerId.toLong())
        val newUuidStr: String = newUuid.toString()
        val newName: String = newUuidStr.substring(newUuidStr.length - 16).replace("-", "_")

        plugin
            .getLogger()
            .log(
                Level.INFO,
                "auth multiplex request from player $name connecting from ${connection.socketAddress}",
            )

        val multiplexedPlayer = MultiplexedPlayer(multiplexerId, name, newName, uuid, newUuid)
        if (!implementationSpecificAuth(multiplexedPlayer)) {
            return
        }

        when (destination) {
            PreLoginDestination.MULTIPLEXED_UUIDS ->
                plugin.multiplexedUuids[multiplexedPlayer.newUuid] = multiplexedPlayer.originalUuid

            PreLoginDestination.PENDING_MULTIPLEXED_LOGINS ->
                plugin.pendingMultiplexerLogins[uuid] = multiplexedPlayer
        }
    }

    /**
     * Performs implementation-specific authentication handshake for a multiplexed player.
     *
     * @param multiplexedPlayer generated multiplexed player details.
     * @return `true` when authentication was successful.
     */
    abstract fun implementationSpecificAuth(multiplexedPlayer: MultiplexedPlayer?): Boolean

    /** Where to send the details of a PreLoginEvent */
    /** Defines where generated multiplex login state should be stored. */
    enum class PreLoginDestination {
        /** Store mappings in [VaneProxyPlugin.multiplexedUuids]. */
        MULTIPLEXED_UUIDS,

        /** Store pending login state in [VaneProxyPlugin.pendingMultiplexerLogins]. */
        PENDING_MULTIPLEXED_LOGINS,
    }

    /**
     * Holds original and generated identity information for auth multiplexing.
     *
     * @property multiplexerId configured multiplexer id.
     * @property name original player name.
     * @property newName generated multiplexed player name.
     * @property originalUuid original player UUID.
     * @property newUuid generated multiplexed UUID.
     */
    class MultiplexedPlayer(
        @JvmField var multiplexerId: Int,
        @JvmField var name: String,
        @JvmField var newName: String,
        @JvmField var originalUuid: UUID,
        @JvmField var newUuid: UUID,
    )

    companion object {
        /** Kick message used when a player is not allowed to use the selected multiplexer. */
        var MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK: String =
            "§cYou have no permission to use this auth multiplexer!"

        /**
         * Sends multiplexed player metadata to [server] via plugin messaging.
         *
         * @param server destination backend server.
         * @param multiplexedPlayer multiplexed identity payload.
         */
        @JvmStatic
        fun registerAuthMultiplexPlayer(
            server: IVaneProxyServerInfo,
            multiplexedPlayer: MultiplexedPlayer,
        ) {
            val stream = ByteArrayOutputStream()
            val out = DataOutputStream(stream)

            try {
                out.writeInt(multiplexedPlayer.multiplexerId)
                out.writeUTF(multiplexedPlayer.originalUuid.toString())
                out.writeUTF(multiplexedPlayer.name)
                out.writeUTF(multiplexedPlayer.newUuid.toString())
                out.writeUTF(multiplexedPlayer.newName)
            } catch (e: IOException) {
                // This should not happen in a ByteArrayOutputStream, but log it for diagnostics
                Logger.getLogger(PreLoginEvent::class.java.getName())
                    .log(Level.SEVERE, "Failed to write multiplexed player data", e)
            }

            server.sendData(stream.toByteArray())
        }
    }
}
