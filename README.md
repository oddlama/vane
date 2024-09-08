<p align="center"><img width="auto" src="https://user-images.githubusercontent.com/31919558/221619567-f4d69311-22bb-468d-bfb4-679567cdd1e9.png"></p>

<div align="center">

[![MIT License](https://img.shields.io/badge/license-MIT-informational.svg)](./LICENSE)
[![Join us on Discord](https://img.shields.io/discord/907277628816388106.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/RueJ6A59x2)
[![Features](https://img.shields.io/badge/link-Features-informational.svg)](https://oddlama.github.io/vane/)
[![Installation Guide](https://img.shields.io/badge/wiki-Installation-informational.svg)](https://github.com/oddlama/vane/wiki/Installation-Guide)
[![FAQ](https://img.shields.io/badge/wiki-FAQ-informational.svg)](https://github.com/oddlama/vane/wiki/FAQ)

</div>

# About vane

Vane is a plugin-suite that provides many immersive and lore-friendly additions to vanilla minecraft.
It will run on any [PaperMC](https://papermc.io) based minecraft server.

[**For a comprehensive feature overview, visit the official website.**](https://oddlama.github.io/vane/)

- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/firework_rocket.png"> Lore friendly and properly integrated immersive features.
  Vane augments the classic vanilla experience, and doesn't try to replace it.
  Generally, players are not supposed to notice which features were introduced by vane.
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/wheat.png"> Countless quality-of-life, gameplay and system improvements
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/end_crystal.png"> Fully-configurable. Disable or configure anything you dislike
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/vane-trifles/items/golden_sickle.png"> Several carefully designed custom items
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/vane-enchantments/items/ancient_tome_of_the_gods.png"> Many custom enchantments for a selection of tools, including the elytra
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/ender_pearl.png"> The best portals you can imagine. Expensive but worthwhile for long distance travel. Supports all entities including minecarts!
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/writable_book.png"> A simple but powerful region system for grief protection
- <img width="auto" height="20px" src="https://github.com/oddlama/vane/blob/main/docs/assets/minecraft/textures/item/map.png"> [BlueMap](https://bluemap.bluecolored.de/), [Dynmap](https://www.spigotmc.org/resources/dynmap%C2%AE.274/) and [Pl3xMap](https://github.com/BillyGalbreath/Pl3xMap) integration
- [And a lot more!](https://oddlama.github.io/vane/)

## 📷 Gallery

Visit the [**Gallery on Modrinth**](https://modrinth.com/mod/vane/gallery) to view a selection of features or 
refer to [**the official website**](https://oddlama.github.io/vane/) for a complete overview.

<p float="left">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/29f9caecbf0ae1a5ae7968c9700183332b4b7c42.gif">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/cd70d5c40403ee85a7013fa66eb2888425f659fd.gif">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/ac141dbfc22b9d6c22de38835080bac3250513f5.gif">
</p>
<p float="left">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/44590a85296abbe67a70a161884397dc6309748e.gif">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/3b55531b0620d45722a4c591e87de6bc40887740.gif">
<img width="32%" height="auto" src="https://cdn.modrinth.com/data/698NGGtb/images/5b12bef32c5b544ae7cd807117410b1bc5bd5346.gif">
</p>

(And a lot more!)

## 📥 Installation

<sup><sub>If you are setting up a new server, check out our [Server Installer](https://oddlama.github.io/minecraft-server)
for an easy way to properly set up a minecraft server with vane, autostart, 3D online map, and more awesome features.</sub></sup>

Download all desired jars files from the [Releases](https://github.com/oddlama/vane/releases/latest) and put them into the `plugins/` directory.
Grab the `all-plugins.zip` if you don't want to download each file by hand. You can also download vane from [Modrinth](https://modrinth.com/plugin/vane). Make sure that:

- You are using the correct Paper (or Purpur) server version (compatible versions are listed in the title on the download page)
- You have the latest version of [ProtocolLib](https://ci.dmulloy2.net/job/ProtocolLib/).
- Beware that `vane-velocity` and `vane-plexmap` (plexmap addon) are not classical server plugins! Use them only if you know what you are doing.

For a comprehensive guide, visit the [Installation Guide](https://github.com/oddlama/vane/wiki/Installation-Guide) on the wiki.

## Proxy Installation (vane-velocity)

Download `vane-velocity.jar` and place it in the velocity proxy server's `plugins/` directory. The configuration will be generated on first start.
All configuration is handled in the plugin's `config.toml`.

There are permissions players will need to perform certain actions, such as starting offline
servers and joining servers in maintenance mode. Permissions can be handled by any external
permissions plugin, such as [LuckPerms](https://luckperms.net).

## Building from source

You can, of course, build the plugin yourself. To do that, you need at least JDK 17.

1. Execute `./gradlew build`
2. All resulting jar files that can be used on a server will be in `target/`.

If you experience "peer not authenticated" issues from Gradle, just retry.
Seems to be a skittish integration between Gradle and Maven repositories.

## FAQ

Please refer to the [FAQ](https://github.com/oddlama/vane/wiki/FAQ) in the wiki.

## Acknowledgements & 3rd-party software

I would like to thank the following projects and people maintaining them:

- [Spigot](https://www.spigotmc.org/) for the awesome baseline server software.
- [PaperMC](https://papermc.io/) for the valuable additions missing from Spigot.
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) for the awesome protocol layer library.
- [DynmapCoreAPI](https://github.com/webbukkit/DynmapCoreAPI) (Apache-2.0), dynmap integration
- [BlueMapAPI](https://github.com/BlueMap-Minecraft/BlueMapAPI) (MIT), [BlueMap](https://bluemap.bluecolored.de/) integration
- [Quark](https://quarkmod.net) (CC-BY-NC-SA 3.0) Assets and texts for the Slime Bucket

#### Included software

The following projects will be included in the compilation step:

- [org.json](https://github.com/stleary/JSON-java) (MIT), Java json implementation
- [night-config](https://github.com/TheElectronWill/night-config) (LGPL3), Java configuration library, TOML implementation used
- [ronmamo reflections](https://github.com/ronmamo/reflections) (WTFPL), Java reflection helper
- [PacketWrapper](https://github.com/dmulloy2/PacketWrapper) (LGPL3), only specific parts are included
- [bStats](https://bstats.org/) (LGPL3), plugin metrics
