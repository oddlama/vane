<img align="left" width="auto" height="90" src="./docs/vane.png">

[![MIT License](https://img.shields.io/badge/license-MIT-informational.svg)](./LICENSE)
[![Join us on Discord](https://img.shields.io/discord/907277628816388106.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/RueJ6A59x2)

[![Overview](https://img.shields.io/badge/link-Overview-informational.svg)](https://oddlama.github.io/vane/)
[![Overview](https://img.shields.io/badge/wiki-Installation-informational.svg)](https://github.com/oddlama/vane/wiki/Installation-Guide)
[![Overview](https://img.shields.io/badge/wiki-FAQ-informational.svg)](https://github.com/oddlama/vane/wiki/FAQ)

# About vane

Vane is a group of plugins which provide many
immersive and lore friendly extensions for vanilla minecraft.
It runs on any [papermc](https://papermc.io) based server.

These plugins follow a **strict no-nonsense** rule:
- [x] If you dislike any feature, you can disable it individually.
- [x] Features must be lore friendly, properly integrated and unobtrusive
- [x] Additions should actually be useful
- [x] Augments the classic vanilla experience, and doesn't try to replace it.

## Features

<b>[For a full overview and explanation of all features, visit the online overview.](https://oddlama.github.io/vane/)</b>

All features are modularized and can be disabled independently of each other.
If you don't want a certain feature, simply disable it.

#### Quality of life (vane-trifles)

- [x] Walk faster on grass paths
- [x] Harvest finished crops by right-clicking (or by using a *Sickle*)
- [x] Double doors open simultaneously
- [x] Sort chests with redstone
- [x] Modify individual connections of walls, fences, etc. and easily modify stair shapes using the *File*
- [x] Store XP in bottles
- [x] Limit anvil repair cost to remove 'too-expensive'
- [x] All recipes will be unlocked automatically when joining the server
- [x] Access to gigantic head library (>30000 heads) for decorative purposes, including in-game menu to search and acquire heads by category, tags or name.
- [x] Use the *Portal Scrolls* to get home or back to where you last used a scroll!
- [x] The *Sickle* allows you to harvest a small radius of finished crops. Similar to the hand-harvesting addition, this will drop only product and no seeds.
- [x] The *File* allows you to modify block individual connections of fences, walls, glass panes and blocks alike.
- [x] The *Empty Experience Bottle* allows you to bottle your experience (right click) for later use. Drinking the full bottle will grant you the stored experience.
      Storing experience will cost a small percentage of the bottled xp. It will also calculate in terms of experience and not levels.
- [x] The *Portal Scroll (Home)* will teleport you to your bed, but it only has a limited amount of uses!
- [x] The *Portal Scroll (Unstable)* will teleport you back to the location where you last used a portal scroll.
- [x] The *Reinforced Elytra* allows you to have armor and an Elytra.

#### Core features (vane-core)

- [x] Full localization support, currently provides english (default), german, french and russian.
- [x] Automatically generated resource pack will provide client side translations
- [x] Inaccessible commands will not be shown to players (sends "Unknown Command" instead)

#### Administrative (vane-admin)

- [x] The server can be automatically stopped after a specified duration without players.
      Using this together with *vane-waterfall* to start the server on demand will
      allow you to save server resources while nobody is online.
- [x] Slightly colorized chat message format for better readability
- [x] Convenience commands for time, weather, gamemode, spawn, ...
- [x] Hazard protection against creeper/wither explosions, door breaking, ...
- [x] Stylish world rebuilding after explosion hazards.
- [x] Whitelist for worlds in which the Wither may be spawned

#### Permissions (vane-permissions)

- [x] Builtin lightweight permissions plugin (permission groups, group inheritance, live editing).
      For better control, all default permissions are revoked and need to be added explicitly.
- [x] Players without any permissions cannot alter the world (just look, no touch!)
- [x] Verified players can vouch for new players with `/vouch <player>`, which will assign a configured group to the new player.

#### Multiplayer sleeping (vane-bedtime)

- [x] Skip night with 50% of players sleeping
- [x] Dynmap integration shows player beds

#### Custom enchantments (vane-enchantments)

- [x] Seamless integration of custom enchantments with the vanilla system
- [x] *Wings*: Occasionally boost your elytra mid-air by sneaking.
- [x] *Soulbound*: Soulbound items will be kept on death. Also prevents yeeting your best tool out of existence.
- [x] *Rake*: Tilling farmland again will till the nearest block around it. Use repeatedly on farmland to create a circle.
- [x] *Careless*: Tilling long grass will remove it and till the block below. Useful in combination with *Rake* to automatically till grass with long grass on top.
- [x] *Seeding*: Right click your crops to plant crops of the same type around them. Use multiple times to create a circle.
- [x] *Takeoff*: Double jump to start gliding with your elytra and receive a small boost.
- [x] *Hell Bent*: Don't hurt your head when flying into a wall. Again.
- [x] *Grappling Hook*: Finally put that fishing rod to good use!
- [x] *Leafchopper*: Remove leaves instantly with your axe. Leaves will drop items as if they decayed naturally.
- [x] *Angel*: A legendary enchantment to accelerate your elytra while pressing shift! Gives a more natural feeling than using rockets. Elytra go brrrr! (Replaces *Wings*)
- [x] *Unbreakable*: Well... A legendary enchantment for truly unbreakable items. Extremely expensive. (Replaces *Unbreaking* and *Mending*)

#### Portals (vane-portals)

- [x] Build portals of arbitrary shape and orientation to get around easily (even horizontal!)
- [x] Correctly retains velocity of players, so you can fly through it
- [x] Apply different styles to portals so they fit your building style
- [x] Dynmap integration shows icons for global portals

#### Regions (vane-regions)

- [x] Players can buy a patch of land, and may control certain environmental conditions and player permissions for that area
- [x] Regions created by admins can be used to protect global areas (e.g. spawn).
- [x] Seamless integration into chest-like menus instead of commands.
- [x] Integrates with portals to allow only players with the portal permission to operate portals in the region
- [x] Integrates with dynmap to make regions visible on the online map
- [x] Visual region selection indicator

#### Proxy plugin (vane-waterfall)

- [x] Authentication multiplexing: Grant players any amount of additional accounts (e.g. useful for secondary spectator accounts).
      This works by proxying logins from a different port and changing UUIDs.
- [x] Servers can be started automatically when a player tries to join.
- [x] `ping` command to check server ping as recieved by the proxy.
- [x] `maintenance` command to schedule maintenance times. While maintenance is active, players without a bypass permission can't join servers.

#### Remarks

> **[as of 1.16.2]** Showing enchantment names inside the enchantment table doesn't work due to protocol limitations.
> Custom enchantments will be shown without any tooltip, and therefore cannot be distinguished from one another.

> **[as of 1.16.2]** Vane technically supports enchantments to be acquired using the enchanting table, but all enchantments from this plugin will be acquired by treasure or crafting.
> The reason is that adding all these enchantments at high levels would not work out well regarding the relative probability of common and rare enchantments.
> At the time of writing, there are only 4 rarity categories, and the vanilla rarities cannot be conveniently modified. So as the custom enchantments are
> on average more powerful than the vanilla ones, we would need more categories to balance the enchanting process.
> This system is not suited for giving out valuable traits (like *Angel* or *Unbreakable*), so we decided to create crafting recipes and modify loot tables instead.

## Installation

For a comprehensive guide, visit the [Installation Guide](https://github.com/oddlama/vane/wiki/Installation-Guide) on the wiki.

To install vane, simply downlaod all desired jars files from the [Releases](https://github.com/oddlama/vane/releases/latest) and put them into the `plugins/` directory.
You can grab the `all-plugins.zip` if you don't want to download each file by hand.
Then start your server and enjoy playing!

### Installation (vane-waterfall)

Again, download/compile and place `vane-waterfall` jar into the proxy server's `plugins/` directory.
The configuration will be generated on first start.

To enable the authentication multiplexing, you need to do the following:

1. Define multiple listeners (distinct ports) for your servers in the proxy's `config.yml`.
2. Map these ports to multiplexer ids in `plugins/vane-waterfall/config.yml`. The specific multiplexer ids must be >0.
   Other than that the ids are only important to check player permissions.
3. Assign the corresponding permission(s) `vane_waterfall.auth_multiplexer.<multiplexer_id>` to the desired group in the proxy's `config.yml`.
4. Assign the group to a player using the players' **UUID**. *THIS IS IMPORTANT* and won't work with player names.

Optionally, you can configure the second configuration section in `plugins/vane-waterfall/config.yml` to allow automatic server start.
An example is provided in the config file.

## Building from source

You can of course build the plugin yourself. To do that you need at least JDK 17.

1. Execute `./gradlew build`
2. All resulting jar files that can be used on a server will be in `target/`.

If you experience "peer not authenticated" issues from gradle, just retry.
Seems to be a skittish integration between gradle and maven repositories.

## FAQ

Please refer to the [FAQ](https://github.com/oddlama/vane/wiki/FAQ) in the wiki.

## Acknowledgements & 3rd-party software

I would like to thank the following projects and people maintaining them:

- [Spigot](https://www.spigotmc.org/) for the awesome baseline server software.
- [PaperMC](https://papermc.io/) for the valuable additions missing from Spigot, and for Waterfall.
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) for the awesome protocol layer library.
- [DynmapCoreAPI](https://github.com/webbukkit/DynmapCoreAPI) (Apache-2.0), dynmap integration
- [BlueMapAPI](https://github.com/BlueMap-Minecraft/BlueMapAPI) (MIT), [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) integration

#### Included software

The following software packets will be included in the compilation step:

- [org.json](https://github.com/stleary/JSON-java) (MIT), Java json implementation
- [ronmamo reflections](https://github.com/ronmamo/reflections) (WTFPL), Java reflection helper
- [PacketWrapper](https://github.com/dmulloy2/PacketWrapper) (LGPL3), only specific parts are included
- [bStats](https://bstats.org/) (LGPL3), plugin metrics
