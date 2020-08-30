## About vane

Vane is a group of plugins which provide many
immersive and lore friendly extensions for vanilla minecraft.
It runs on any [papermc](https://papermc.io) based server.

These plugins follow a **strict no-nonsense** rule:
- [x] Only actually useful additions
- [x] Features must be lore friendly, properly integrated and unobstrusive
- [x] Everything can be disabled individually
- [x] Augments the classic vanilla experience

### Features

All features are modularized and can be disabled independently of each other.
So if you don't want a certain feature, simply disable it.

#### Little things & quality of life

- [x] Walk faster on grass paths
- [x] Harvest finished crops by right-clicking
- [x] Double doors open simultaneously
- [ ] Craft lights that automatically turn on when it's night
- [ ] Sort chests with redstone
- [x] Skip night with 50% of players sleeping
- [ ] Store XP in bottles (with slight loss)
- [ ] Modify individual connections of walls, fences, panes, etc.
- [ ] All recipes are automatically unlocked when joining the server

#### Administrative and 'meta' features

- [x] Full localization support, currently provides english (default) and german.
- [x] Builtin lightweight permissions plugin (permission groups, group inheritance, live editing).
      For better control, all default permissions are revoked and need to be added explicitly.
- [x] The server can be automatically stopped after a specified duration without players. Using this together with **(TODO insert waterfall plugin)** will allow you to save server resources while nobody is online.
- [x] Inaccessible commands will not be shown to players (sends "Unknown Command" instead)
- [x] Slightly colorized chat message format for better readability
- [x] Convenience commands for time, weather, gamemode, spawn, ...
- [x] Hazard protection against creeper/wither explosions, door breaking, ...
- [x] Players without any permissions cannot alter the world (just look, no touch!)
- [ ] Advancements to guide you through most additions
- [x] Client side translations for custom items and enchantments

#### Custom enchantments

While vane's API allows enchantments to be acquired using the enchanting table,
most (TODO all??) enchantments from this plugin will be acquired by treasure
or crafting, for the simple reason that late-game enchanting is too easy,
and therefore not well suited for giving out valuable traits (like *Angel* or *Unbreakable*).

- [x] Seamless integration of custom enchantments
- [x] *Wings*: Occasionally boost your elytra mid-air by sneaking
- [x] *Careless*: Tilling long grass will remove it and till the block below
- [ ] *Soulbound*: Soulbound items will be kept on death. They also can't be dropped accidentally.
- [x] *Rake*: Tilling farmland again will till the nearest block around it. Use multiple times to slowly create a circle. Works in combination with Careless to till grass with long grass on top.
- [x] *Seeding*: Right click your crops to plant crops of the same type around them. Use multiple times to create a circle.
- [x] *Take Off*: Double jump to start gliding with your elytra and receive a small boost.
- [x] *Hell Bent*: Don't hurt your head when flying into a wall. Again.
- [x] *Grappling Hook*: Finally put your fishing rod to good use!
- [x] *Leafchopper*: Remove leaves instantly with your axe. Leaves will drop items as if they decayed naturally.
- [x] *Angel*: A legendary enchantment to accelerate your elytra while pressing shift! Gives a more natural feeling than using rockets. Elytra go brrrr! (Replaces *Wings*)
- [x] *Unbreakable*: Well... A legendary enchantment for truly unbreakable items. Extremely rare. (Replaces *Unbreaking* and *Mending*)

Remarks:

> [as of 1.16.2] Showing enchantment names inside the enchantment table doesn't work due to protocol limitations. Custom enchantments will be shown without any tooltip, and therefore cannot be distinguished from one another.

#### Custom items

- [ ] The *Sickle* allows you to harvest a small radius of finished crops. Similar to the hand-harvesting addition, this will drop only product and no seeds.
- [ ] The *File* allows you to modify block individual connections of fences, walls, glass panes and blocks alike.

#### Unique features

Spectator accounts:
- Grant players additional spectator accounts (login to a different port)

Portals:

Build portals of any shape any any orientation
to get around more easily.

Regions:

Allow players to own a patch of land, and control certain
environmental conditions inside, as well as other players permissions.
Permission integration allows subtractive merging of permissions when in a region (can only take permissions away)

#### Administrative utilities

Privilege system & Graylist:

Grant privileges to players (or have other players invite new players
by letting them vouch for the new player)





### Building from source

You need Java 11 or higher.

- Copy `cache/patched_{version}.jar` from a paper server to `libs/`.
- Execute `./gradlew build && ./gradlew shadowJar`

#### IDEAS

- full netherite armor for one enchantment 
- dragon egg (catalyst?)
- nether star
- pillager books (recept of unbreakable)


portal ancient debris block like


invisible item frame visible if nothing in it


armor stand manipulation by perpendicular intersection between Line of sight and arm 

spawn with remnants of old world



javadoc comment code (or at least semi public api),
then generate javadoc


/verify system for players -> adds permission groups


per chunk cache for regions, like "chunk is fully in region"

check achievement on anvil


!!!automatic recipes unlocking

sign editor tool.


NO TOO EXPENSIVE! set cost at anvil output anvil.


harvest with any right click that isnt a tools

wither in overworld = big nono

shulker box right click with item to add?

playerlist footer and header

TODO licenses of included shadowed libraries
