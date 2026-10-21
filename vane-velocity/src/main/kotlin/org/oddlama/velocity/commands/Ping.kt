package org.oddlama.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import org.oddlama.vane.proxycore.commands.ProxyPingCommand
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender
import org.oddlama.velocity.compat.VelocityCompatProxyPlayer

/**
 * Velocity command adapter for the proxy-core ping command.
 *
 * @param plugin active Velocity plugin instance.
 */
class Ping(plugin: Velocity) : SimpleCommand {
    /** Backing proxy-core command implementation. */
    private val cmd = ProxyPingCommand("vane_proxy.commands.ping", plugin)

    /**
     * Executes the ping command using the appropriate command-sender wrapper.
     *
     * @param invocation Velocity command invocation context.
     */
    override fun execute(invocation: SimpleCommand.Invocation) {
        val sender = invocation.source()
        val compatSender =
            if (sender is Player) {
                VelocityCompatProxyPlayer(sender)
            } else {
                VelocityCompatProxyCommandSender(sender)
            }
        cmd.execute(compatSender, invocation.arguments())
    }
}
