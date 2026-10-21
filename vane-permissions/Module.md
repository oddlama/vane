# Module vane-permissions

A configuration-driven permission group system with inheritance, plus a vouching mechanism for
promoting players.

[org.oddlama.vane.permissions.Permissions] reads groups from configuration, flattens their
inheritance into a concrete permission set per group, and applies the result to players through
runtime Bukkit permission attachments. Because attachments are rebuilt rather than mutated, group
changes take effect without restarting.

The module can optionally strip _all_ default permissions from every source — including other
plugins and vanilla Minecraft — so that the configured groups become the only thing granting
access. This is deliberately opt-in: enabling it causes OPed players to lose commands unless the
permissions are granted back explicitly.

# Package org.oddlama.vane.permissions

Module entry point, group definitions, inheritance flattening and attachment management.

# Package org.oddlama.vane.permissions.argumentTypes

Command argument type that completes and resolves configured permission group names.

# Package org.oddlama.vane.permissions.commands

The `permission` and `vouch` commands.
