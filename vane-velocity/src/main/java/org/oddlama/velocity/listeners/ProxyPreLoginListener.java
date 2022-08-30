package org.oddlama.velocity.listeners;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.event.VelocityCompatPreLoginEvent;

public class ProxyPreLoginListener {

	Velocity velocity;

	@Inject
	public ProxyPreLoginListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe
	public void pre_login(final com.velocitypowered.api.event.connection.PreLoginEvent event) {
		PreLoginEvent proxy_event = new VelocityCompatPreLoginEvent(velocity, event);
		proxy_event.fire();
	}

}
