```toml
title = "Command Hider"
icon = "minecraft:spider_eye"
module = "vane-core"
```
---
Do you hate it that players can infer the commands and plugins installed on your server?
This is possible because they get a different error message for unknown commands
as opposed to just lacking the required permission.

Vane replaces the error message with the exact `unknown-command` message from your
`spigot.yml`, which prevents leaking any information. Additionally, tab completion
will also only be changed to only show commands to which a player has access.
