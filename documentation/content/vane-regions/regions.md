```toml
title = "Regions"
icon = "minecraft:map"
module = "vane-regions"
```
---
## Overview

Vane's region module allows you to purchase a piece of land.
This allow you assign rights to other players, like building rights or container rights,
as well as to set some environmental conditions within the region (disable explosions, monster spawn, PVP, ...).

## Permission Management

There are some important concepts related to regions, which allow you to manage multiple regions easily:

- A **region** is an actual purchased piece of land.
- A so-called **region-group** is where you configure the environmental permissions and player permissions.
- Multiple regions can be governed by the same permissions simply by assinging them to the same region-group.

Each region belongs to exactly one region-group which determines the rules in that region.
If you create a new region, it will belong to your default default region-group. This is a region-group that exists
by default and where only you have permissions to do anything.

To avoid having to give each player permissions separately, **roles** exists to manage player permissions.
Available permissions are intentionally kept simple to allow quick configuration, and should be everything
your players need. For more sophisticated permission control, use WorldGuard or another plugin.

- 3 default roles are created in each region-group for your convenience: **admin**, **friend**, **others**.
	- **others** cannot build, can only view containers (only look, no touch!), but can use things (buttons, ...)
	- **friends** can build, can use things, can use chests
	- **admins** can do everything including permission management.
- Exactly one role can be assigned to each player in that region-group. Unassigned players automatically belong to the **others** role.

## Portals & Regions

If a portal is inside of a region, some additional things apply:

- Admins of the region can change the portal settings
- The visibility of the portal can be restricted to either players that have the portal permission or other portals that are in a region with the same region group.
