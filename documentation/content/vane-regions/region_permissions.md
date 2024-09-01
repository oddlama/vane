```toml
title = "Region Permission Management"
icon = "minecraft:iron_bars"
module = "vane-regions"
```
---
## Default Group

![](images/region_default_group.png)

Newly created regions are initially assigned to your default region-group.
In summary, this region-group allows you to do anything and restrict everyone else from building or using your chests.
Refer to [Regions](#feature-vane-regions--regions) for more information about the default permissions.

It's generally a good idea to keep your private regions in this default group.
Add your friends to the "friends" role to allow them to build and use chests.

## Creating a Region-Group

![](images/region_create_group.png)

Only create a new region-group if you require entirely different settings for one or more regions.
This is usually the case when you share a bigger project with other people, and want everyone to be an admin in related areas.

If you require a new region-group, you can create one by clicking the respective item on the right side in the region menu `/rg`.

## Assign Group

![](images/region_group_assign.png)

Finally, you need to assign your region to the newly created group, which you can do from the region's settings menu.

**Tip:** If you open the region menu `/rg` while standing in a region, you will see direct links to both the region's settings and the currently assigned region-group.

## Environmental Settings

![](images/region_group_settings.png)

Each region-group has a set of environmental settings that apply to the actual regions in this region-group themselves and not to any players.
These are settings like: Disabling monster spawn, controlling whether PVP is allowed, whether fire spreads or similar things.
You can see and change all settings in the menu related to the region-group.

## Roles

![](images/region_role_settings_and_assign.png)

Players are assigned to exactly one role that dictates what they are allowed to do.
Any newly created region-group by default contains 3 roles: **\[Admins\]**, **\[Others\]** and **Friends**.

**\[Others\]** is a catch-all role for all players that have no assigned role. Those players may only use things like doors and buttons, but are restricted from building or using containers.

**Friends** may build, use things and use containers, but cannot change settings.

**\[Admins\]** are allowed to do everything, including assigning roles to players and adjusting permissions.

## Assigning Roles and Permissions

![](images/region_role_assign_dialog.png)

By clicking on a role in the region-menu, you can customize the permissions for all players that belong to that role.
In the same menu, you can assign players to that the selected role.
