## About vane

Vane is a plugin which provides many
immersive and lore friendly extensions for vanilla minecraft.
It runs on any [papermc](https://papermc.io) based server.

Vane follows a **strict no-nonsense** rule.
Every feature must have a real value to the players or the admins,
and is designed to augment the classic vanilla experience in a lore friendly way.
All features can be disabled independently.
So while there will be some administrative tools, a lot of features will just be
simple things and quality of life changes, with
only very few greater (but generally appreciated) features like regions and portals.

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

#### Administrative and 'meta' features

- [x] Full localization support, currently provides english (default) and german.
- [x] Builtin lightweight permissions plugin (permission groups, group inheritance, live editing)
- [x] All default permissions are revoked and need to be added explicitly
- [x] Inaccessible commands will not be shown to players (sends "Unknown Command" instead)
- [x] Slightly colorized chat message format for better readability
- [x] Convenience commands for time, weather, gamemode, spawn, ...
- [x] Hazard protection against creeper/wither explosions, door breaking, ...
- [x] Players without any permissions cannot alter the world (just look, no touch!)
- [ ] Advancements to guide you through most additions

#### Custom enchantments

- [x] Seamless integration of custom enchantments (with [API](TODO)!)
  - (as of 1.16.2) Showing enchantment names inside the enchantment table doesn't work due to protocol limitations. Names will be blank.
- [x] Client side translations for custom items and enchantments

#### Unique features

Lore friendly custom enchantments:

Harvest additions:
- Rake adjacent grass fields by raking farmland again
- Harvest multiple plants at once
- Plant more seeds easily

Elytra enhancements:
- Speed boost on take-off
- From occasional mid-air speed boost to full 
- Rare permantent controllable boost enchant (haha elytra go brrrrrr)

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

- Automatically stop the server after a certain time of player-emptyness.

Privilege system & Graylist:

By default everyone can join and have a look,
but that person will not be able to do anything except walking around.

Grant privileges to players (or have other players invite new players
by letting them vouch for the new player)





#### IDEAS

- permission groups
  - vane.trifles.command.home (item!! -> back death?)
  - vane.trifles.command.spawn

- full netherite armor for one enchantment 
- dragon egg (catalyst?)
- nether star
- pillager books (recept of unbreakable)


portal ancient debris block like


invisible item frame visible if nothing in it


spawn with remnants of old world



javadoc comment code (or at least semi public api),
then generate javadoc


/verify system for players -> adds permission groups


per chunk cache for regions, like "chunk is fully in region"

check achievement on anvil


/ihaterecipes -> get recipes


/<module> reload to reload config for all!!!!! automatic.

todo datapack and language from yaml!


playerlist footer and header
