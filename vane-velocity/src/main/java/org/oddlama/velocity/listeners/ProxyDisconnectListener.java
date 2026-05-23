package org.oddlama.velocity.listeners;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.oddlama.velocity.Velocity;

public final class ProxyDisconnectListener {

    final Velocity velocity;

    @Inject
    public ProxyDisconnectListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe(priority = 0)
    public void disconnect(DisconnectEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        velocity.get_multiplexed_uuids().remove(uuid);
    }
}
