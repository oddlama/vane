```toml
[feature]
title = "Graylist (optional)"
icon = "minecraft:chain"
module = "vane-admin"
```
---
Vane provides a feature similar to a graylist.
This allows anyone to connect to your server but in a *no touch, only look!* kind of way.
To modify anything about the world, a player must either be opped,
or be assigned to the user group with `perm add player_name user`, or any higher group.
To enable the graylist, set the following option:

```yaml
# server/plugins/vane-admin/config.yml
world_protection:
  enabled: true
```

> Why can't I build anything?

This feature is **disabled** by default, as it often caused confusion.
