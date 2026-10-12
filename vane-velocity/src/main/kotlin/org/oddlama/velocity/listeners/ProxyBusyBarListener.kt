package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import org.oddlama.velocity.Velocity

/**
 * Tells the BUSY Bar feature about players joining and leaving the network and switching servers.
 *
 * @property velocity plugin instance holding the BUSY Bar feature.
 */
class ProxyBusyBarListener @Inject constructor(private val velocity: Velocity) {
    /**
     * Announces a player who finished logging in.
     *
     * @param event Velocity post-login event.
     */
    @Subscribe
    fun postLogin(event: PostLoginEvent) {
        val player = event.player
        val textures = player.gameProfile.properties.firstOrNull { it.name == "textures" }?.value
        velocity.busyBar.onPlayerJoin(player.uniqueId, player.username, textures)
    }

    /**
     * Announces a player leaving, if they had logged in.
     *
     * @param event Velocity disconnect event.
     */
    @Subscribe
    fun disconnect(event: DisconnectEvent) {
        val loggedIn = event.loginStatus == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN ||
            event.loginStatus == DisconnectEvent.LoginStatus.PRE_SERVER_JOIN
        if (loggedIn) velocity.busyBar.onPlayerQuit(event.player.uniqueId, event.player.username)
    }

    /**
     * Records the backend a player is now on.
     *
     * @param event Velocity server post-connect event.
     */
    @Subscribe
    fun serverPostConnect(event: ServerPostConnectEvent) {
        val server = event.player.currentServer.orElse(null) ?: return
        velocity.busyBar.onServerSwitch(event.player.uniqueId, server.serverInfo.name)
    }
}
