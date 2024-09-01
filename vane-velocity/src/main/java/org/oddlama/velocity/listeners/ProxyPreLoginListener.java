package org.oddlama.velocity.listeners;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.event.VelocityCompatPreLoginEvent;

public class ProxyPreLoginListener {

	final Velocity velocity;

	@Inject
	public ProxyPreLoginListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe
	public void pre_login(final com.velocitypowered.api.event.connection.PreLoginEvent event) {
		PreLoginEvent proxy_event = new VelocityCompatPreLoginEvent(velocity, event);

		// For Velocity, our multiplexer connections need more work; they
		// later get handled in `ProxyGameProfileRequestListener`
		proxy_event.fire(PreLoginEvent.PreLoginDestination.PENDING_MULTIPLEXED_LOGINS);
	}

}
