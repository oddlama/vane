```toml
[feature]
title = "Create Regions"
icon = "minecraft:textures/item/writable_book.png"
category = "mechanics"
module = "vane-regions"
```
---
#### 1 Preparation

![](images/region_create.png)

A region costs diamonds. When you remove the region again, you get your diamonds back. Start a region selection by opening the region menu with `/rg` and selecting **Create Region** on the left.

![](images/region_selection.png)

![](images/region_create_confirm.png)

#### 2 Select & Create Region

Next, choose your region by selecting 2 opposite corners by **left-** and **right-clicking** with an empty hand. Your region selection will be displayed live in the world. If all requirements are met, you will see green particles together with your selection, otherwise red ones. Open the region menu again to check what is missing or to continue creating the region. If you are satisfied with your selection, create the region by clicking on the left button in the menu.

#### 3 Default Group

![](images/region_default_group.png)

A new region is assigned to your default group at the beginning. The settings there are preset so that you yourself are allowed to do everything, everyone else on the other hand can only use things (doors, ...), but neither build nor use containers.

#### 4 Create Group

![](images/region_create_group.png)

If you want to customize these settings, it is recommended to create a new region group. Otherwise, these settings will automatically apply to new regions you create in the future, as they will also be assigned to your default group at the beginning. Create a new region-group on the right side of the regions menu `/rg`.

#### 5 Region Settings

![](images/region_group_settings.png)

There are a few settings that do not affect players, but the region itself. These include, for example, whether monsters spawn in the region, whether PVP is allowed, or whether fire spreads. You can see and change all settings in the menu of the region group.

#### 6 Roles

![](images/region_role_settings_and_assign.png)

A new group contains 3 roles at the beginning: **\[Admins\]**, **\[Others\]** and **Friends**.

**\[Admins\]** are allowed to do everything, including assigning roles to players and adjusting permissions.

**Friends** are also allowed to do everything, but cannot change settings.

**\[Others\]** is a catch-all group for all players who are not assigned to any other role. These may only use things, but may not build or use containers.

#### 7 Assign Player

![](images/region_role_assign_dialog.png)

In the menu of your region group, assign the appropriate roles to your friends. In the same menu, you can also customize what your friends may do. For example, you can take away their right to use containers, or take away their building rights. If you only want to do this for a few of your friends, just create a new role for them.

#### 8 Assign Group

![](images/region_group_assign.png)

Finally, you just need to assign your region to the correct group. This is done via the region menu.

**Tip:** If you open the region menu `/rg` while you are in a region, you will see the corresponding region and the assigned region group directly in the main menu.
