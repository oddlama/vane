## About vane

Vane is a group of plugins which provide many
immersive and lore friendly extensions for vanilla minecraft.
It runs on any [papermc](https://papermc.io) based server.

These plugins follow a **strict no-nonsense** rule:
- [x] If you dislike any feature, you can disable it individually.
- [x] Features must be lore friendly, properly integrated and unobstrusive
- [x] Additions should actually be useful
- [x] Augments the classic vanilla experience, and doesn't try to replace it.

### Features

All features are modularized and can be disabled independently of each other.
If you don't want a certain feature, simply disable it.

#### Little things & quality of life

- [x] Walk faster on grass paths
- [x] Harvest finished crops by right-clicking (or by using a *Sickle*)
- [x] Double doors open simultaneously
- [x] Sort chests with redstone
- [x] Modify individual connections of walls, fences, etc. and easily modify stair shapes using the *File*
- [x] Skip night with 50% of players sleeping
- [x] Store XP in bottles
- [x] Limit anvil repair cost to remove 'too-expensive'
- [x] All recipes will be unlocked automatically when joining the server

#### Administrative and 'meta' features

- [x] Full localization support, currently provides english (default) and german.
- [x] Automatically generated resource pack will provide client side translations
- [x] Builtin lightweight permissions plugin (permission groups, group inheritance, live editing).
      For better control, all default permissions are revoked and need to be added explicitly.
- [x] The server can be automatically stopped after a specified duration without players. Using this together with **(TODO insert waterfall plugin)** will allow you to save server resources while nobody is online.
- [x] Inaccessible commands will not be shown to players (sends "Unknown Command" instead)
- [x] Slightly colorized chat message format for better readability
- [x] Convenience commands for time, weather, gamemode, spawn, ...
- [x] Hazard protection against creeper/wither explosions, door breaking, ...
- [x] Stylish world rebuilding after explosion hazards.
- [x] Whitelist for worlds in which the Wither may be spawned
- [x] Players without any permissions cannot alter the world (just look, no touch!)

#### Custom enchantments

While vane technically supports enchantments to be acquired using the enchanting table,
all enchantments from this plugin will be acquired by treasure or crafting,
for the simple reason that late-game enchanting is too cheap,
and therefore not well suited for giving out valuable traits (like *Angel* or *Unbreakable*).

- [x] Seamless integration of custom enchantments with the vanilla system
- [x] *Wings*: Occasionally boost your elytra mid-air by sneaking.
- [x] *Careless*: Tilling long grass will remove it and till the block below.
- [x] *Soulbound*: Soulbound items will be kept on death. Also prevents yeeting your best tool out of existence.
- [x] *Rake*: Tilling farmland again will till the nearest block around it. Use multiple times to slowly create a circle. Works in combination with Careless to till grass with long grass on top.
- [x] *Seeding*: Right click your crops to plant crops of the same type around them. Use multiple times to create a circle.
- [x] *Take Off*: Double jump to start gliding with your elytra and receive a small boost.
- [x] *Hell Bent*: Don't hurt your head when flying into a wall. Again.
- [x] *Grappling Hook*: Finally put that fishing rod to good use!
- [x] *Leafchopper*: Remove leaves instantly with your axe. Leaves will drop items as if they decayed naturally.
- [x] *Angel*: A legendary enchantment to accelerate your elytra while pressing shift! Gives a more natural feeling than using rockets. Elytra go brrrr! (Replaces *Wings*)
- [x] *Unbreakable*: Well... A legendary enchantment for truly unbreakable items. Extremely rare. (Replaces *Unbreaking* and *Mending*)

#### Custom items

- [x] The *Sickle* allows you to harvest a small radius of finished crops. Similar to the hand-harvesting addition, this will drop only product and no seeds.
- [x] The *File* allows you to modify block individual connections of fences, walls, glass panes and blocks alike.
- [x] The *Empty Experience Bottle* allows you to bottle your experience (right click) for later use. Drinking the full bottle will grant you the stored experience.
      Storing experience will cost a small percentage of the bottled xp. It will also calculate in terms of experience and not levels.

#### Portals

- [ ] Build portals of arbitrary shape and orientation to get around easily (even horizontal!)
- [ ] Integrates with regions to control portal connection access
- [ ] Apply different styles so your portal fits your building style
- [ ] Dynmap integration shows icons for global portals

#### Regions

- [ ] Players can buy an arbitrarily shaped patch of land, and may control certain environmental conditions and player permissions for that area
- [ ] Server-owned regions can be used to protect gobal areas (e.g. spawn).
- [ ] Visual selection of any 2D polygon shape with arbitrary heights.
- [ ] Seamless integration into chest-like menus instead of commands.

#### Spectator accounts

- [ ] Grant players additional spectator accounts (by proxying logins from a different port via **Waterfall**)

#### Remarks

> [as of 1.16.2] Showing enchantment names inside the enchantment table doesn't work due to protocol limitations. Custom enchantments will be shown without any tooltip, and therefore cannot be distinguished from one another.

### Installation

**This plugin requires Java 11!**

To install vane, begin by downloading and placing all desired module jars into the `plugins/` directory.

1. Start the server to generate configuration files, and edit them to your preference.
2. Either restart the server, or type `/vane reload` to apply the changes.
3. Execute `/vane generate_resource_pack`. This will place the required resource pack in the server's working directory.
4. Copy the resource pack to a publicly accessible webserver and configure the related section in `plugins/vane-core/config.yml`.
5. Either restart the server again, or type `/vane reload` to apply the changes.

You are done! Enjoy playing!

> [as of 1.16.2] Beware that the minecraft client currently has issues with webservers that serve resource packs via https and don't allow ssl3.
> This protocol is considered insecure and therefore should NOT be used. To workaround this issue, you should host the file in a http context.
> Using http is not a security issue, as the file will be verified via its sha1 sum by the client.

Thats it!

### Building from source

You can of course build the plugin yourself. To do that you need at least JDK 11.
Before you can proceed, you need to copy the fully patched Paper server jar
to `libs/`. This is required so the compiler can find minecraft-native symbols.

1. Copy `cache/patched_{version}.jar` from a paper server to `libs/` (create folder if necessary).
2. Execute `./gradlew build`
3. Resulting jar files will be in `target/'.

### 3rd-party software

TODO include licenses
- protocollib wrappers
- shadowed things
