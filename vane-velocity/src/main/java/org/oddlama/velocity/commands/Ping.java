package org.oddlama.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.oddlama.vane.proxycore.commands.ProxyPingCommand;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender;
import org.oddlama.velocity.compat.VelocityCompatProxyPlayer;

public class Ping implements SimpleCommand {

    ProxyPingCommand cmd;

    public Ping(final Velocity plugin) {
        this.cmd = new ProxyPingCommand("vane_proxy.commands.ping", plugin);
    }

    @Override
    public void execute(Invocation invocation) {
        final var sender = invocation.source();
        cmd.execute(
            sender instanceof final Player player
                ? new VelocityCompatProxyPlayer(player)
                : new VelocityCompatProxyCommandSender(sender),
            invocation.arguments()
        );
    }
}
