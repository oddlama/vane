# Module vane-proxy-core

Platform-independent implementation of vane's proxy-side features. This module contains no proxy
API imports of its own — it defines abstractions that a platform module such as `vane-velocity`
implements.

It is one of the few vane modules that do not depend on Paper or `vane-core`, along with
`vane-velocity` and `vane-busybar-common`; the build excludes them from the paperweight and core
dependency groups for exactly that reason.

## The abstraction boundary

`VaneProxyPlugin` is the base every platform entry point extends. Alongside it,
`ProxyPlayer`, `ProxyServer`, `ProxyPendingConnection`, the `listeners` event types, the
`scheduler` types and `IVaneLogger` each describe a capability that proxies provide but describe
differently. A platform module supplies a thin adapter for each — `vane-velocity`'s `compat`
package is the reference implementation.

Adding a feature here means writing it against these interfaces only. Anything that needs a
platform type belongs in the platform module instead.

## Features

`Maintenance` implements scheduled maintenance mode: it persists the schedule, denies logins while
active, and notifies players. `BusyBarProxy` serves BUSY Bar bridges for the network when
`[BusyBar]` is enabled; see the vane-busybar module documentation. `AuthMultiplex` in the config package supports mapping several
accounts onto one identity.

# Package org.oddlama.vane.proxycore

Entry point abstraction and the core proxy models — player, server, pending connection — plus the
maintenance feature.

# Package org.oddlama.vane.proxycore.busybar

The proxy side of vane-busybar: `BusyBarProxy` serves BUSY Bar bridges for the whole network, fed by
the `BackendLinkServer` that backends report their events, grants and snapshots to. The bridge
server, TLS and link protocol come from `vane-busybar-common`, which vane-velocity shades along.

# Package org.oddlama.vane.proxycore.commands

Platform-independent proxy commands (`maintenance`, `ping`, `busybar`) and the command sender abstraction they
are dispatched through.

# Package org.oddlama.vane.proxycore.config

Proxy configuration: the config model and manager, managed server definitions, the server info
interface, and auth multiplexing.

# Package org.oddlama.vane.proxycore.listeners

Abstract event types — login, pre-login, ping — that platform modules translate their native events
into.

# Package org.oddlama.vane.proxycore.log

Logging abstraction with adapters for both `java.util.logging` and SLF4J, since proxies differ in
which they expose.

# Package org.oddlama.vane.proxycore.scheduler

Task scheduling abstraction, implemented per platform.

# Package org.oddlama.vane.proxycore.util

Helpers duplicated from `vane-core`'s utilities, because this module intentionally does not depend
on it.
