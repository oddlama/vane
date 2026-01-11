```toml
title = "Graylist (optional)"
icon = "minecraft:chain"
module = "vane-admin"
```
---
## Graylist

Vane provides a feature similar to a graylist.
This allows anyone to connect to your server but in a *no touch, only look!* kind of way.
To modify anything about the world, a player must either be oped,
or have the `vane.admin.modify_world` permission.

If you are using the included lightweight permissions plugin `vane-permissions`,
you can assign a new player to the `user` group by executing `perm add player_name user`.

> Why can't I build anything?

This feature is **disabled** by default, as it often caused confusion.

### Quick start

- Enable in `vane-admin` config: set `world_protection.enabled: true`, then run `/vane reload`.
- Grant a newcomer build rights with `perm add <player> user`.
- Or allow trusted members to vouch: give them `verified` via `perm add <player> verified`, then they can `/vouch <other_player>`.

### Commands

- `/perm add <player> user` — allow building (bypasses graylist).
- `/perm add <player> verified` — let a player vouch others.
- `/vouch <player>` — move a player to the `user` group.

## Vouching

When using `vane-permissions`, you can also allow trusted members of your community to vouch for other players.
This allows them to lift a new user into the `users` group,
without requiring an admin.
Beneficial to permit your friends 
to invite other people they know.
Vane stores who vouched for whom.

To give players this permission, assign them to the verified group with `perm add player_name verified`.
They may now vouch for other users by using `/vouch other_player`.
