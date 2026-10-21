## Updating vane to a new minecraft release

There are some things that need to be done to make vane compatible with a new minecraft release. All work happens on
the `main` branch.

1. First, we need to compile against the newest Paper dev bundle. All Paper plugin modules get it from the version
   catalog, so there is exactly one place to change: bump `paper` in `gradle/libs.versions.toml`, e.g.
   `paper = "26.3.build.+"` → `paper = "26.4.build.+"`. The `+` always picks up the newest build of that release.

   If paperweight complains about the dev bundle, also check `paperweightUserdev` in the same file against the latest
   version (see [their test plugin](https://github.com/PaperMC/paperweight-test-plugin/blob/master/build.gradle.kts)).
   A new Paper release may also raise the required JDK; the toolchain is set via `jvmToolchain(25)` in the root
   `build.gradle.kts` and `java-version` in `.github/workflows/*.yml`.

2. If any other dependencies seem out of date (dynmap, bluemap, packetevents, Geyser, Velocity, ...), bump them in
   `gradle/libs.versions.toml` as well. Renovate usually opens PRs for these, so check the open ones first. Whether
   this is necessary depends on whether the respective API changed in that release, so it is on-demand. If you forget
   something, you'll notice when testing with the new version later on.

3. `./gradlew build`. There will probably be compiler errors, but that's fine. This makes sure the new dev bundle
   resolves and nothing else is missing before we get to the actual code.

4. Read what changed. Look at the [Paper announcements](https://papermc.io/news) and the Paper commit that performed
   the update (its diff shows exactly how CraftBukkit/NMS call sites were adapted). Sometimes there is nothing to do,
   sometimes a lot. Sometimes there is no error, but things should still be adjusted (e.g., deprecations).

   Since Paper dropped CraftBukkit package relocation, imports like `org.bukkit.craftbukkit.CraftWorld` no longer
   carry a version (`v1_xx_Rx`) and don't need to be touched.

5. Fix the compiler errors. Vane depends on Mojang's internal code, so there will be changes that nobody documents.
   Most of it lives in a handful of files; find them with `rg -l "net.minecraft|org.bukkit.craftbukkit"`:

   - `vane-core/src/main/kotlin/org/oddlama/vane/util/Nms.kt`
   - `vane-core/src/main/kotlin/org/oddlama/vane/util/ItemUtil.kt`
   - `vane-core/src/main/kotlin/org/oddlama/vane/util/BlockUtil.kt`
   - `vane-core/src/main/kotlin/org/oddlama/vane/core/item/VanillaFunctionalityInhibitor.kt`
   - `vane-trifles/src/main/kotlin/org/oddlama/vane/trifles/items/Trowel.kt`
   - `vane-portals/src/main/kotlin/org/oddlama/vane/portals/entity/FloatingItem.kt`

   Minecraft is no longer obfuscated, and the paperweight dev bundle ships the server sources with Mojang names, so
   you no longer need BuildTools or mapping files: use your IDE's "go to definition" on the broken symbol to read the
   new server code, and search for usages of the old API to see how vanilla/Paper uses it now.

   **Example:** 26.2 → 26.3 (`8d8990c0`), 4 errors:

   ```text
   e: file:///.../vane-core/src/main/kotlin/org/oddlama/vane/util/ItemUtil.kt: Unresolved reference 'asCraftMirror'.
   ```

   **Solution:**

    - Jump into `CraftItemStack` in the dev bundle sources.
    - Observe that `asCraftMirror` is gone and `asBukkitMirror` takes its place with the same signature.
    - Check the Paper update commit to confirm it is a rename and not a behavior change.
    - The other errors of that update were of the same kind: `VanillaRegistries.createLookup` → `createWorldLookup`,
      `Entity.setInvulnerable` → `setPermanentlyInvulnerable`, and the `ItemStack.hurtAndBreak` break callback now
      receives an `ItemStack`.

   Often it's a rename, but sometimes things just cease to exist, and you need to understand what the code in vane
   wants to do and figure out a new way to do it. That's the price to pay for doing things that the official API
   doesn't support.

6. `./gradlew build` again. If you still have errors, re-apply (5) until done. While it does compile, we are not yet
   finished.

7. Check the runtime reflection. Some values are not exposed at all and are accessed by field name at runtime, which
   the compiler cannot check. Find them with `rg -n "getDeclaredField"`. Most hits are vane's own classes (portals,
   regions serialization) and are safe; the ones that matter are:

   - `Core.unfreezeRegistries()` in `vane-core/src/main/kotlin/org/oddlama/vane/core/Core.kt`, which accesses
     `MappedRegistry.frozen` and `MappedRegistry.unregisteredIntrusiveHolders`.
   - `vane-velocity/src/main/kotlin/org/oddlama/velocity/Velocity.kt` (field `cm` of the Velocity server; only relevant
     on Velocity updates).

   Open the class in the dev bundle sources and make sure the fields still exist under that name and type. A wrong name
   compiles fine but fails at startup with `Failed to unfreeze registries` in the log.

8. Update the resource pack format. Vane generates its own resource pack, and its `pack.mcmeta` declares which pack
   formats it supports. Read the changelog to see whether anything crucial (file layout, item model definitions) has
   changed and adjust the resource pack generator (`vane-core/.../core/resourcepack/ResourcePackGenerator.kt`) if
   necessary. Usually you only need to raise `max_format` in `vane-core/src/main/resources/resource_pack/pack.mcmeta`
   to the [newest value](https://minecraft.wiki/w/Pack_format). You'll see in testing whether that worked or not.

9. Make a commit detailing the update and what issues were encountered, if any (e.g. `build: update to Paper 26.4`,
   following [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), which CI lints on PRs). Don't
   push yet.

10. Now comes testing. `./gradlew runServer` builds all plugins and starts a Paper test server in `run/` with them
    loaded (the `cleanVane*` and `cleanWorld` tasks in the `run paper` group reset its state). Disable resource pack
    distribution in the vane-core config, generate the pack with `/vane generate_resource_pack` and copy
    `run/VaneResourcePack.zip` to your client.

    Enter the server. Now it is important to test those parts of vane that interface with Mojang's internals, since
    those are the most likely to break.

    - `/customitem give vane_trifles:golden_sickle` should display as a sickle and work when used on wheat. (Tests
      custom item registration and event dispatching)
    - Take an elytra in your hand, run `/enchant vane_enchantments:angel`, test whether you can speed up by sneaking.
      (Tests custom enchantment registration in the bootstrapper)
    - Duplicate the elytra, go into survival mode (IMPORTANT!) then combine them using an anvil. You should get Angel
      II.
    - Take a smithing table, combine the elytra with a netherite ingot. (Tests complex smithing recipe integration)
    - Put some random blocks and items in a chest, place a button next to it and press it. The chest should now be
      sorted.
    - If Geyser is involved, join with a Bedrock client and check that vane items show their textures and names.

    Fix issues and make a new commit if necessary.

11. Release. Order matters here, because `vane-geyser-extension` converts `docs/resourcepacks/v<version>.zip` into its
    Bedrock pack at build time and the build fails if that file doesn't exist:

    - Copy the generated resource pack to `docs/resourcepacks/v<new_version>.zip`.
    - Bump `version` in the root `build.gradle.kts` (always bump a minor version for Minecraft updates) **and** in
      `vane-geyser-extension/gradle.properties`. Both must match.
    - `./gradlew build` once more to make sure everything, including the Bedrock pack, builds.
    - Commit and tag: `git commit -m 'chore: version bump' && git tag -a -m '' v<new_version>` (use `-S`/`-s` to sign
      if you have a key set up). Tags with a suffix like `-beta.1` are published as pre-releases.

12. Push: `git push && git push --tags`.

    Pushing the tag runs `.github/workflows/release.yml`, which checks that the tag matches the version in
    `build.gradle.kts`, builds everything, creates `all-plugins.zip` and uploads the jars to a **draft** GitHub
    release with generated notes. The resource pack is served from GitHub Pages (`docs/`), where vane-core downloads it
    via `resourcePackUrl` in `vane-core.properties`.

13. Edit the draft release notes on GitHub (see `git log` for what changed, and previous releases for the format) and
    publish it. Afterward, entertain modrinth.com and hangar.papermc.io.

Congratulations, you are now awake and can start implementing new features.
