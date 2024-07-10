```toml
title = "Portal Construction"
icon = "minecraft:crying_obsidian"
module = "vane-portals"
```
---
## Materials

Before you can build a portal, you need to gather the following materials:

| Item | Amount | Description |
|------|--------|-------------|
| {{ item:minecraft:enchanting_table }} | 1 | Portal console |
| {{ item:minecraft:netherite_block }} | 1 | Origin block |
| {{ item:minecraft:lever }} | 1 | A switch |
| {{ item:minecraft:obsidian }} {{ item:minecraft:crying_obsidian }} {{ item:minecraft:gold_block }} {{ item:minecraft:gilded_blackstone }} {{ item:minecraft:emerald_block }} | Several | Portal frame blocks |

The portal frame may be built from any of the boundary blocks.
It doesn't matter which ones you use, but remember that each block-type
can later be individually styled as another block.

## Portal Frame

![](images/portal_boundary.png)
![](assets/gifs/portal-frame-construction.gif)

First, you need to construct the frame of the portal.
It can be built either vertically or horizontally,
but typically a vertical variant will be more convenient to use.

A portal frame is formed by a closed loop of blocks of any shape.
Like a nether portal, it doesn't count edge-blocks, but only blocks
that touch the inside of the portal with at least one side.
The enclosed air will become the portal area.

The netherite block, also called the origin block, marks the block on which a traveling player arrives.
There have to be at least 1x3 blocks of portal area above the netherite block, so a player can pass through
without suffocating. With horizontal portals, the nethrite block location doesn't matter as the player always arrives in the middle.

## Console & Lever

![](assets/gifs/portal-create.gif)

Next, you need to place the enchantment table in a convenient place near your portal.
It will become the portal console which is used to select the destination before you travel.
Create the portal by shift-right-clicking on the enchantment table,
and then right-click on the frame. If all necessary conditions are met,
you will be prompted to enter a name for the portal.

Finally, place your lever on any portal frame block,
or on a block near the console (a 3x3x3 cube with the console in the middle) so you can activate it.

Note that the portal is private by default, which means that only you can use it as the destination
from other portals.
To change that, right-click the console to open the menu and change the visibility in the settings.

> If you want, you can add additional consoles in the same way as you added the first console.

## Activation

![](assets/gifs/portal-activate-use.gif)

Activate the portal by first selecting a destination in the console, and then activate the lever.
If the destination portal is private, the destination selection will be reset afterwards.
If you selected a public portal, it will stay selected after the portal deactivates.

Portals can also be activated by a redstone repeater facing directly into a portal block.
The portal is activated on a rising signal edge.
Deactivation occurs after 10 seconds as usual.
This only works if the destination portal is not in a vane-region that restricts portal access from the public!

> It can be very handy to activate a portal from a detector rail ;)
