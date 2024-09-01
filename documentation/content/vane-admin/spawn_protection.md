```toml
title = "Spawn Protection (optional)"
icon = "minecraft:iron_bars"
module = "vane-admin"
```
---
Vane provides slightly more sophisticated spawn protection than what is available in vanilla.
It defines an actual radius (not a square) around some block to be protected from player modification.

By default, players are still allowed to use things like doors, chests and buttons.
Players can be exempt from the protection by granting them the permission
'vane.admin.bypass_spawn_protection.'
By default, is given to ops and members of the `admin` group.

If you need even more control, use a [Region](#feature-vane-regions--regions).

> Why can't I build anything?

This feature is **disabled** by default, as it often caused confusion.
