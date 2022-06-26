```toml
[feature]
title = "Portal Construction"
icon = "minecraft:textures/block/obsidian.png"
module = "vane-portals"
```
---
## Materials

![](images/portal_materials.png)

To build a portal, you will need the following materials:

- 1x Enchantment Table
- 1x Netherite Block
- 1x Lever
- Any selection of boundary blocks for the frame:<br>
  {{ item:minecraft:obsidian }} {{ item:minecraft:crying_obsidian }} {{ item:minecraft:gold_block }} {{ item:minecraft:gilded_blackstone }} {{ item:minecraft:emerald_block }}

The portal frame may be built from any of the boundary blocks.
It doesn't matter which ones you use, but remember that each block-type
can later be individually styled as another block.

## Portal Frame

![](images/portal_boundary.png)

First you should construct the portal frame. For this, you will use the respective blocks from the material list. The portal can be built either standing or lying, but for portals that are walked through, the standing variant is recommended. For a portal frame to be valid, it must be "watertight". It must therefore completely enclose an air-area in one plane. This air-area becomes the portal area, and all blocks that enclose and touch this area will form the portal frame. Corner blocks, therefore, do not count as part of the portal at all.

Use the netherite block to mark the block on which a player arrives. The shape of the portal can be any shape, as long as there are at least 1x3 blocks free above the netherite block for a player to pass through. With lying portals, the player always comes out in the middle.

## Console & Lever

Finally, you need the enchantment table as a console to select the target and change settings later. You can put the switch on any frame block, or on a block near the console (3x3x3 cube with console in the middle).

You may use multiple levers. Later (after creating) you can add more consoles if necessary by selecting them with shift-right click and binding them to the portal frame with right click.

## Create Portal

Create the portal by first shift-right-clicking to select the console, and then right-clicking to select the frame. If all conditions are met, you will be prompted to enter a name for the portal. Once you have done this, the portal is mostly ready.

Note that the portal is private by default, which means that only you can set the destination, as well as select it as a destination on other portals.

## Activate

Activate the portal by first selecting a destination, and then activating the lever. If the destination portal is private, the destination selection will be reset afterwards. The portal can also be activated via a redstone repeater directly into a portal block, but only if it is a publicly usable portal!
