package org.oddlama.velocity.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import org.oddlama.vane.proxycore.commands.ProxyBusyBarCommand
import org.oddlama.vane.proxycore.commands.ProxyCommandSender
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender
import org.oddlama.velocity.compat.VelocityCompatProxyPlayer

/**
 * Velocity command adapter for `/busybar`.
 *
 * @param plugin active Velocity plugin instance.
 */
class BusyBar(plugin: Velocity) : SimpleCommand {
    /**
     * Backing proxy-core command implementation.
     */
    private val cmd = ProxyBusyBarCommand(plugin)

    /**
     * Executes the command for a player or the console.
     *
     * @param invocation Velocity command invocation context.
     */
    override fun execute(invocation: SimpleCommand.Invocation) {
        cmd.execute(wrap(invocation.source()), invocation.arguments())
    }

    /**
     * Suggests subcommands and player names.
     *
     * @param invocation Velocity command invocation context.
     * @return completions for the last argument.
     */
    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val args = invocation.arguments().toMutableList()
        if (args.isEmpty()) args += ""
        return cmd.suggest(wrap(invocation.source()), args)
    }

    /** Players need their identity, everyone else only receives messages. */
    private fun wrap(source: CommandSource): ProxyCommandSender =
        if (source is Player) VelocityCompatProxyPlayer(source) else VelocityCompatProxyCommandSender(source)
}
