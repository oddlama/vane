package org.oddlama.velocity.commands

import com.velocitypowered.api.command.SimpleCommand
import org.oddlama.vane.proxycore.commands.ProxyMaintenanceCommand
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender

/**
 * Velocity command adapter for maintenance mode management.
 *
 * @param plugin active Velocity plugin instance.
 */
class Maintenance(plugin: Velocity) : SimpleCommand {
    /** Backing proxy-core command implementation. */
    private val cmd = ProxyMaintenanceCommand("vane_proxy.commands.maintenance", plugin)

    /**
     * Executes the maintenance command as a generic command source.
     *
     * @param invocation Velocity command invocation context.
     */
    override fun execute(invocation: SimpleCommand.Invocation) {
        cmd.execute(VelocityCompatProxyCommandSender(invocation.source()), invocation.arguments())
    }
}
