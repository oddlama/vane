## Updating vane to a new minecraft release

There are some things that need to be done to make vane compatible with a new minecraft release. All work happens on
a feature branch based on the current default branch.

1. First, we need to compile against the newest Paper dev bundle. All Paper plugin modules get it from the version
   catalog, so there is exactly one place to change: bump `paper` in `gradle/libs.versions.toml`, e.g.
   `paper = "26.3.build.207-stable"` → the exact new stable build. Do not use `+` selectors.
   Update the corresponding Paper/Mojang URLs and hashes in `nix/paper.nix` for the development runtime.

    If paperweight complains about the dev bundle, also check `paperweightUserdev` in the same file against the latest
    version (see [their test plugin](https://github.com/PaperMC/paperweight-test-plugin/blob/master/build.gradle.kts)).
    A new Paper release may also raise the required JDK; the toolchain is set via `jvmToolchain(25)` in the root
    `build.gradle.kts` and `nix/toolchain.nix`. CI inherits the toolchain from the flake.

2. If any other dependencies seem out of date (dynmap, bluemap, packetevents, Geyser, Velocity, ...), bump them in
   `gradle/libs.versions.toml` as well. Renovate usually opens PRs for these, so check the open ones first. Whether
   this is necessary depends on whether the respective API changed in that release, so it is on-demand. If you forget
   something, you'll notice when testing with the new version later on.

3. Enter `nix develop` and run `gradle build` for incremental feedback. After resolving compiler/dependency
   issues, run `nix run .#update-gradle-deps`, review the URL/hash changes and validate with `nix flake check -L`.
   See `nix/README.md` for why sandboxed builds replay dependencies rather than using the user cache.

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

10. Now comes runtime testing, with disposable worlds and explicit EULA acceptance. Inside `nix develop`,
    `VANE_ACCEPT_EULA=true testserver` incrementally builds dirty sources and starts the pinned Paper server in
    `.vane-testserver/`. A Paper upgrade requires a fresh `--state-dir`; never erase a live world to make a test pass.
    Disable resource pack distribution in the vane-core config, generate the pack with `/vane generate_resource_pack`
    and copy that state's `VaneResourcePack.zip` to your client.

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
    - Bump `version.txt` (a minor version for Minecraft updates). No separate Geyser version needs synchronizing.
    - Run `nix fmt`, `nix flake check -L` and `nix build .#release`.
    - Commit the version/pack and create a matching `v<new_version>` tag using your normal maintainer workflow.
      Signing is an explicit maintainer operation, never performed by the Nix build. Prerelease suffixes are supported.

12. Push the reviewed branch and tag when ready. Tag CI runs the flake checks/build and invokes the Nix-packaged
    GitHub publisher to create a **draft**. The version, clean source revision and remote tag must match.
    Existing published assets are never silently overwritten. Pages builds the website and serves all historical
    resource packs at their unchanged URLs.

13. Review and publish the GitHub draft. To publish the same reviewed artifacts/notes on Modrinth, use the manual
    release workflow with `modrinth: true`, or the explicitly confirmed publisher described in `nix/README.md`.
    The default `nix run .#publish-modrinth` is an offline dry-run. Configure the `release` environment approvals
    and runtime token first. Hangar publication remains a separate maintainer action.

Congratulations, you are now awake and can start implementing new features.
