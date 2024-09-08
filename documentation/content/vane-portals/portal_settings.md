```toml
title = "Portal Settings"
icon = "minecraft:name_tag"
module = "vane-portals"
```
---
## Portal Settings

![](images/portal_menus.png)

You as the owner of the portal will see additional entries in the menu to personalize or delete the portal.
The settings menu allows you to change several things about the portal:

**Name:** Change the name of your portal

**Icon:** Choose any item from your inventory as the portal icon.
This item will be used in the target selector, and will be displayed above consoles of portals where it is set as the destination.
You can also use one of the many decorative heads from `/heads`.

![](images/portal_style.png)

![](assets/gifs/portal-style.gif)

**Style:** In the style selector, you can either use one of the predefined styles or create your own.
Every type of block used in the portal frame can be disguised as any other block—at no cost.
By default, everything is made of obsidian. You can even apply a different block based on whether the portal is currently active or not.
Please share the styles you've created on our [discord](https://discord.gg/RueJ6A59x2)!

> Remember to press **Apply** at the end to make your changes visible.

<!---->
> You can hide your portal by changing the inactive portal area block.

**Orientation lock:** Activate this to ensure that entities always exit the portal on the front-side. Usually portals behave relative to one another: Enter front, exit front - enter back, exit back.
This locking can be useful when there's a wall behind your portal, and exiting on the back makes no sense.

**Visibility:** Choose who can use this portal as a destination. There are several options, but the two group options are only available when the portal's origin block is located inside a vane-group:

- Private: Only you, the owner
- Public: Everyone
- Group: Any players who can theoretically use portals in the respective region
- Group-internal: Anyone, but only if the used source portal is in the same region

**Target-lock**: Prevents the selected destination portal from resetting after the portal closes when the destination is not public. Useful for hidden portals in your base that have a set destination or
are part of a minecart railway network.
