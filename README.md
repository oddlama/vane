<p align="center"><img width="auto" height="90" src="./docs/vane.png"></p>

<div align="center">

[![MIT License](https://img.shields.io/badge/license-MIT-informational.svg)](./LICENSE)
[![Join us on Discord](https://img.shields.io/discord/907277628816388106.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/RueJ6A59x2)
[![Features](https://img.shields.io/badge/link-Features-informational.svg)](https://oddlama.github.io/vane/)
[![Installation Guide](https://img.shields.io/badge/wiki-Installation-informational.svg)](https://github.com/oddlama/vane/wiki/Installation-Guide)
[![FAQ](https://img.shields.io/badge/wiki-FAQ-informational.svg)](https://github.com/oddlama/vane/wiki/FAQ)

</div>

# About vane

Vane is a plugin-suite which provides many immersive and lore friendly additions to vanilla minecraft.
It will run on any [PaperMC](https://papermc.io) based minecraft server.

[**For a comprehensive feature overview, visit the official website.**](https://oddlama.github.io/vane/)

- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/firework_rocket.png"> Lore friendly and properly integrated immersive features.
  Vane augments the classic vanilla experience, and doesn't try to replace it.
  Generally, player's are not supposed to notice which features were introduced by vane.
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/wheat.png"> Countless quality-of-life, gameplay and system improvements
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/end_crystal.png"> Fully-configurable. Disable or configure anything you dislike
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/vane-trifles/items/golden_sickle.png"> Several carefully designed custom items
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/vane-enchantments/items/ancient_tome_of_the_gods.png"> Many custom enchantments for a selection of tools, including the elytra
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/ender_pearl.png"> The best portals you can imagine. Expensive but worthwile for long distance travel. Supports all entities including minecarts!
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/writable_book.png"> A simple but powerful region system for grief protection
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/map.png"> [BlueMap](https://bluemap.bluecolored.de/) and [Dynmap](https://www.spigotmc.org/resources/dynmap%C2%AE.274/) integration
- [And a lot more!](https://oddlama.github.io/vane/)

## Installation

If you are setting up a new server, check out our [Server Installer](https://oddlama.github.io/minecraft-server)
for an easy way to properly setup a minecraft server with vane, autostart, 3D online map, and more awesome features.

Otherwise, simply download all desired jars files from the [Releases](https://github.com/oddlama/vane/releases/latest) and put them into the `plugins/` directory.
Grab the `all-plugins.zip` if you don't want to download each file by hand.
You also need the newest version of [ProtocolLib](https://ci.dmulloy2.net/job/ProtocolLib/).

For a comprehensive guide, visit the [Installation Guide](https://github.com/oddlama/vane/wiki/Installation-Guide) on the wiki.

## Installation (vane-waterfall)

Download `vane-waterfall.jar` into the proxy server's `plugins/` directory.
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
- [BlueMapAPI](https://github.com/BlueMap-Minecraft/BlueMapAPI) (MIT), [BlueMap](https://bluemap.bluecolored.de/) integration
- [Quark](https://quarkmod.net) (CC-BY-NC-SA 3.0) Assets and texts for the Slime Bucket

#### Included software

The following projects will be included in the compilation step:

- [org.json](https://github.com/stleary/JSON-java) (MIT), Java json implementation
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (Apache-2.0), Java YAML 1.1 implementation
- [ronmamo reflections](https://github.com/ronmamo/reflections) (WTFPL), Java reflection helper
- [PacketWrapper](https://github.com/dmulloy2/PacketWrapper) (LGPL3), only specific parts are included
- [bStats](https://bstats.org/) (LGPL3), plugin metrics
