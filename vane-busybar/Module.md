# Module vane-busybar

Streams server and vane events to [BUSY Bar](https://busy.bar) desk displays and lets a bar
report its focus mode back to the server.

The server never connects to a bar. Each player runs a small bridge next to their device; the bridge
opens an outbound connection to [org.oddlama.vane.busybar.EventStream] and draws the events on the
bar over the local network. This works behind NAT and keeps players' home addresses off the server.

The bridge is a Python program in [`vane-busybar/bridge`](bridge/README.md), released alongside the
plugin with the same version. Its README covers player setup.

## Protocol

Players create a token with `/busybar link`, which shows it once inside a pairing string (see
[TLS](#tls)). Only a SHA-256 hash of it is stored. Every endpoint requires
`Authorization: Bearer <token>`.

`GET /events` is a server-sent-events stream. The first event is always `snapshot`; an idle stream
receives a `: ping` comment every 15 seconds.

| Event                                            | Sent to                                   | Source                                                                         |
| ------------------------------------------------ | ----------------------------------------- | ------------------------------------------------------------------------------ |
| `snapshot`                                       | the connecting bridge                     | on connect                                                                     |
| `player.join`, `player.quit`                     | everyone                                  | Paper                                                                          |
| `bedtime.enter`, `bedtime.leave`, `bedtime.skip` | everyone                                  | Paper, threshold from vane-bedtime                                             |
| `world.time`                                     | everyone                                  | time jumps (commands, night skips), and every 30 s                             |
| `server.tps`                                     | everyone                                  | every `TpsInterval` seconds                                                    |
| `server.stop`                                    | everyone                                  | shutdown, sent once before players are kicked (their quits are not reported)   |
| `presence.updated`                               | the player                                | after `presence.set`                                                           |
| `permissions.updated`                            | the player                                | the player's grants changed                                                    |
| `autostop.scheduled`, `autostop.aborted`         | everyone                                  | vane-admin                                                                     |
| `portal.activate`                                | the traveller                             | vane-portals                                                                   |
| `portal.destroy`                                 | the portal owner                          | vane-portals                                                                   |
| `advancement`                                    | everyone                                  | advancements announced in chat, one event per player per second with a `count` |
| `slimechunk.enter`, `slimechunk.leave`           | the player, while carrying a slime bucket | vane-trifles item, checked every second                                        |
| `region.enter`, `region.leave`                   | the player                                | vane-regions, checked every second                                             |
| `region.visitor`                                 | the region owner                          | strangers entering, first at once, then batched per minute                     |

`GET /icon` returns the server icon (`server-icon.png`) as PNG, or `404` when the server has
none. The snapshot's `icon` field carries the same ETag, so a bridge only downloads the icon again
after it is changed and can send `If-None-Match` to get a `304`. Bridges shrink it for the display.

`GET /head/<id>` returns a player's face as a 8x8 PNG, the hat layer drawn over it. `player.join`
and `player.quit` carry the skin id as `head` (null on offline-mode servers). Only skins of players
seen since the server started can be requested, so the endpoint never fetches arbitrary URLs. The
server downloads each skin from textures.minecraft.net once and caches the face.

`POST /action` accepts one JSON request, answering `202` when it was queued for the next tick:

- `{"type":"presence.set","busy":true}` toggles focus mode, which adds a tab list tag.
- `{"type":"action.invoke","action":"autostop.abort"}` cancels a pending autostop.

Each request type can be disabled through `AllowedActions`.

With vane-regions, `region.enter` carries `region`, `owner`, `own`, `pvp` and `may_build`, and
the snapshot's `region` field holds the same object for the region the player is in, or null.
`region.visitor` carries `region`, up to five `visitors` by name, their total `count` and the
first visitor's `head`. Only strangers count as visitors: the owner, players whose role may build,
and visitors while the owner stands in the region themselves, are left out. A region's first
visitor is reported at once; later ones are collected for a minute and reported together, and
each visitor counts at most once per region in ten minutes. Since it reveals where other players
are, it needs `vane.busybar.receive.region_visitors` (ops only by default) and can be turned off
for everyone with `RegionVisitorAlerts`.

## TLS

The stream serves HTTPS by default, without a domain, a certificate authority, or renewals. The
`Tls` option picks one of three modes:

| `Tls`            | Certificate                                                                      | Bridges verify it by                   |
| ---------------- | -------------------------------------------------------------------------------- | -------------------------------------- |
| `auto` (default) | Self-signed P-256, generated on first start as `tls-auto.key` and `tls-auto.crt` | pinning the key fingerprint            |
| `keystore`       | PKCS#12 file in `TlsKeystore`, for example converted from Let's Encrypt          | the usual certificate authority checks |
| `off`            | None, plain HTTP: for a TLS reverse proxy in front of the port or a trusted LAN  | the proxy's certificate                |

`/busybar link` shows a pairing string with everything a bridge needs:

```
<token>@203.0.113.7:9123#sha256=<fingerprint>
```

A bridge reads it as a URL with `https://` implied; only with `Tls` `off` does it start with an
explicit `http://`, so a bridge never falls back to plain HTTP on its own. The part before `@` is
the bearer token, sent as `Authorization: Bearer <token>`. The `sha256` part, present only in
`auto` mode, is the base64url SHA-256 of the certificate's DER `SubjectPublicKeyInfo`. With it, the
bridge accepts the server only if its certificate's key hashes to it and ignores the certificate's
name, issuer, and validity; this is what `curl --pinnedpubkey sha256//<base64>` checks (after
converting base64url to base64). Without it, the bridge validates the certificate normally.

The address comes from `PublicUrl`, or else from `server-ip` in `server.properties` and `Port`. A
bare host in `PublicUrl` gets `Port`; a full URL without a port gets `Port` too, unless `Tls` is
`off`, where it may point at a reverse proxy. The plugin cannot know which address bridges reach it
at (NAT, proxies, DNS names), so when both are empty, it warns on startup, and `/busybar link` asks
the player to contact an admin instead of issuing a token. The fingerprint identifies the server's
key, not its address, so a player may swap the address for another one leading to the same server,
such as `localhost` on the server's own machine.

The fingerprint reaches the player inside Minecraft's own encrypted connection, so nobody between
bridge and server can swap in another certificate. On offline-mode servers that connection is not
encrypted, and the plugin warns about it on startup.

The certificate never expires, and the key only changes through `/busybar rotatekey`, which also
disconnects every bridge until its owner links again. Delete both files to have them generated anew
on the next start; a damaged pair stops the stream with an error until then.

## Permissions

A bridge only receives what its owner may see and only sends what its owner may do.
`AllowedActions` in the config switches a request off for everyone; the permissions below decide
per player or group.

| Permission                             | Default  | Grants                                             |
| -------------------------------------- | -------- | -------------------------------------------------- |
| `vane.busybar.receive.players`         | everyone | `player.join`, `player.quit`                       |
| `vane.busybar.receive.bedtime`         | everyone | `bedtime.*` , `world.time`                         |
| `vane.busybar.receive.portals`         | everyone | `portal.*` for your own portals                    |
| `vane.busybar.receive.advancements`    | everyone | `advancement`                                      |
| `vane.busybar.receive.slimechunk`      | everyone | `slimechunk.*`, only while carrying a slime bucket |
| `vane.busybar.receive.regions`         | everyone | `region.enter`, `region.leave`                     |
| `vane.busybar.receive.region_visitors` | op       | `region.visitor` for your own regions              |
| `vane.busybar.receive.server`          | op       | `server.tps`                                       |
| `vane.busybar.receive.autostop`        | op       | `autostop.*`                                       |
| `vane.busybar.action.presence`         | everyone | `presence.set`                                     |
| `vane.busybar.action.autostop_abort`   | op       | `autostop.abort`                                   |

`vane.busybar.receive.*` and `vane.busybar.action.*` grant a whole group. `snapshot`,
`presence.updated`, `permissions.updated` and `server.stop` are always delivered, and the
snapshot leaves out fields its owner may not see.

Bridges stay connected while their owner is offline, but Bukkit can only evaluate permissions of
online players. Grants are therefore recorded on join, on quit, and once a minute while online.
With vane-permissions installed, offline players are also re-evaluated once a minute from their
stored groups, so `/perms` changes reach their bridge within a minute. With any other permission
plugin, the recorded grants apply while the player is away, and a change made while they are
offline takes effect the next time they join. When the grants of a connected player change, their
bridge receives `permissions.updated` with the new list.

## Commands

| Command                         | Who                  | Does                                                                |
| ------------------------------- | -------------------- | ------------------------------------------------------------------- |
| `/busybar` or `/busybar status` | players              | Shows whether a bridge is linked and connected, and focus mode.     |
| `/busybar link`                 | players              | Issues a new token, shown once, replacing any previous one.         |
| `/busybar unlink`               | players              | Revokes the token, disconnects the bridge and turns focus mode off. |
| `/busybar busy on\|off`         | players              | Switches focus mode from the game; the player's Bar follows.        |
| `/busybar list`                 | `vane.busybar.admin` | Lists players with a token or focus mode on.                        |
| `/busybar clear <player>`       | `vane.busybar.admin` | Turns off a player's focus mode, also when they are offline.        |
| `/busybar revoke <player>`      | `vane.busybar.admin` | Revokes a player's token and turns their focus mode off.            |
| `/busybar rotatekey`            | `vane.busybar.admin` | Replaces the `auto` TLS key; every player has to link again.        |

The command itself needs `vane.busybar.commands.busybar` (everyone by default).
`vane.busybar.admin` defaults to op; the admin subcommands also work from the console.

## Behind a Velocity proxy

On a network behind Velocity with vane-velocity, the proxy can serve every bridge, so a BUSY Bar
follows its owner across servers with a single pairing:

1. In the proxy's vane-velocity `config.toml`, set `[BusyBar] Enabled = true` and `PublicUrl`, and
   `[BusyBar.Link] Secret` to a random string (`openssl rand -base64 32`).
2. On each backend, set `ProxyLinkAddress` to the proxy's `BusyBar.Link` address,
   `ProxyLinkServerName` to the server's name in `velocity.toml`, and `ProxyLinkSecret` to the same
   secret.
3. Players run `/busybar link` on the proxy. Tokens from a backend's own stream no longer work.

The proxy then owns tokens, focus mode, TLS (with the same `Tls` modes, keys in its plugin folder)
and `/busybar`, which it answers in English. It announces players joining and leaving the network
itself, sends a bridge a fresh `snapshot` when its owner switches servers, and `server.stop` with a
`server` field when that backend shuts down or is lost. Everything else comes from the backends:
each event carries a `server` field and reaches the bridges whose owner should see it. World time,
bedtime and TPS only go to players on that server, or last on it while offline.

A proxied backend serves no bridges itself; its `/busybar` only points to the proxy's. It opens one
TCP connection to the proxy carrying one JSON object per line, and reconnects with backoff when it
drops. In the handshake, each side proves with an HMAC over the other's nonce that it knows the
secret, which is never sent. The backend forwards its events with their audience and the
permission they need, and reports each player's grants, which the proxy filters by; players no
backend has reported yet get the default grants. Permissions can differ between servers, so the
proxy goes by the server a player is on: reports about offline players only count from the
server they were last on. The proxy asks for the backend's part of
snapshots, runs `autostop.abort` (bridges name the stopping `server`), mirrors focus mode to every
backend for the tab list tag, and tells backends which players have a bridge connected, since
slime chunk and region visitor checks only run for those. The proxy side lives in
`org.oddlama.vane.proxycore.busybar`, the shared code in `vane-busybar-common`.

## Optional integrations

vane-admin, vane-bedtime, vane-permissions, vane-portals, and vane-regions are optional. Their classes are only touched from
`org.oddlama.vane.busybar.hooks`, and each hook is created after its plugin was found enabled.

# Package org.oddlama.vane.busybar

Module entry point, the event stream server, vanilla event forwarding, and focus-mode presence.

# Package org.oddlama.vane.busybar.commands

The `/busybar` command for linking a bridge.

# Package org.oddlama.vane.busybar.link

The link reporting this backend to a Velocity proxy running vane-velocity, which then serves the
bridges.

# Package org.oddlama.vane.busybar.hooks

Integrations with other vane modules, loaded only when those modules are installed.
