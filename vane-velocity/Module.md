# Module vane-velocity

The Velocity implementation of `vane-proxy-core`. Note that this module lives under
`org.oddlama.velocity`, not `org.oddlama.vane.velocity`.

Almost all behavior comes from `vane-proxy-core`; what is here is the wiring. `Velocity` is the
plugin entry point, constructed by Velocity's dependency injection with the proxy server, logger,
bStats factory and data directory. The `listeners` package subscribes to Velocity's native events
and forwards them into the platform-independent event types, and the `compat` package adapts
Velocity's types onto the `vane-proxy-core` interfaces.

Adding a proxy feature normally means changing `vane-proxy-core`, not this module. Changes belong
here only when they concern Velocity-specific APIs.

# Package org.oddlama.velocity

Plugin entry point and Velocity-specific helpers.

# Package org.oddlama.velocity.commands

Registration of the maintenance, ping and busybar commands with Velocity's command manager.

# Package org.oddlama.velocity.compat

Adapters mapping Velocity's player, server, server info and ping types onto the `vane-proxy-core`
abstractions.

# Package org.oddlama.velocity.compat.event

Adapters wrapping Velocity's login, pre-login, ping and pending-connection events.

# Package org.oddlama.velocity.compat.scheduler

Adapter over Velocity's scheduler implementing the `vane-proxy-core` scheduling interfaces.

# Package org.oddlama.velocity.listeners

Velocity event subscribers that translate native events into `vane-proxy-core` events and dispatch
them.
