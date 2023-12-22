## Updating vane to a new minecraft release

There are some things that need to be done in order
to make vane compatible with a new minecraft release.

1. First of all, we need to compile against the newest paper API and mojang mappings.
   Make sure you are on the latest `develop` branch.

   In `build.gradle.kts` the version of `io.papermc.paperweight.userdev` needs to be bumped
   to the latest one available (I usually just look at [their test plugin](https://github.com/PaperMC/paperweight-test-plugin/blob/master/build.gradle.kts)
   to figure out which version that is). All occurrences of e.g. `paperDevBundle("1.19.3-R0.1-SNAPSHOT")`
   are updated accordingly.

2. If any other dependencies seem out of date (ProtocolLib, dynmap API, bluemap API, ...)
   those should also be bumped. This is not always necessary, since it depends on whether the
   respective API changed in that release - so this is on-demand. If there was a new API and
   you forget to update this, you'll probably run into errors when testing with the new version
   later on. (So you'll notice)

3. `./gradlew build` once. There will be a lot of compile errors, but that's fine.
   This is to ensure that there is nothing else missing and we get to the compilation step now.

4. Point all imports to the new version. All occurrences of `org.bukkit.craftbukkit.v1_19_R2`
   will have to be replaced with `org.bukkit.craftbukkit.v1_19_R3` (or whatever version we are at now).
   That should mostly be limited to just a handful of files. Run `rg -l "v1_19_R2"` to see all files
   where it occurs.

5. `./gradlew build` again. There may still be some errors, depending on whether the API changed or not.
   In any case, definitely read the latest thread on [SpigotMC](https://www.spigotmc.org/) (search for developer notes) to get
   an understanding of what has changed in the bukkit API. Sometime's its nothing, sometimes it's a lot.
   Sometimes there is no error but things should still be adjusted (like when the now-legacy chat coloring was deprecated).

6. The real pain begins now. Since vane depends heavily on mojang's internal API, there might
   be changes that nobody could have anticipated and that will not be documented. Often there
   are breakages regarding entity/enchantment registration, since mojang often restructures things
   in the background 5 times over.

   You will need access to the decompiled code in order to understand many of these errors,
   since there is no documentation and you need to look at how it was used before and how it is used now.
   Open a new folder, grab spigot's `BuildTools.jar` and run `java -jar BuildTools.jar --rev 1.19.4`.
   Beware that this tool is fucking shite and sets your global git name and email if you are using per-repo
   configuration - undo that if necessary. When it's done, you'll have a folder `work/decompile-latest`
   with the source. Also important are `work/bukkit-<someid>-members.csrg` and `work/minecraft_server.1.19.4.txt`
   which contain the remapped names for some functions. We'll come back to that that later.

   **Example:** 1.19.3 -> 1.19.4, 1 error:

   ```java
   > Task :vane-core:compileJava FAILED
   /root/projects/vane/vane-core/build/sources/java/org/oddlama/vane/util/Nms.java:143: error: cannot find symbol
   			final var world_version = SharedConstants.getCurrentVersion().getWorldVersion();
   																			^
   symbol:   method getWorldVersion()
   location: interface WorldVersion
   ```

   **Solution:**

   - View `work/decompile-latest/net/minecraft/server/SharedConstants.java`
   - Follow definition of `getCurrentVersion()` to `WorldVersion.java`
   - Observer that there is no more `getWorldVersion()`.
     `getDataVersion()` and `getProtocolVersion()` look promising as they contain "version".
   - To see what we need, look where we originally used that value. Go to vane source,
     observe that it was used in `DataFixers.getDataFixer().getSchema(HERE)`.
   - Search for uses of `getSchema()` to see how it is used now: `rg getSchema"\(" work/decompile-latest`
   - Observe that it is now used like `DataConverterRegistry.getDataFixer().getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))`.
   - Conclusion: Indeed we need to use `.getDataVersion().getVersion()` instead.

   Seems tedious because it is fucking tedious. That was also still comparatively easy,
   sometimes things just cease to exist and you need to understand what the code in vane
   want's to do and figure out a new way to do it now. That's the price to pay for doing
   things that the official API doesn't support.

7. `./gradlew build` again. If you still have errors, re-apply method from (6) until done.
   For 1.19.3 -> 1.19.4 this was everything and we can continue. While it does compile,
   we are not yet finished.

8. There are some parts of vane that use runtime reflection to access/modify values that
   are not even exposed via mojang's new mappings. Those are currently just two, but it is crucial
   to update them. We basically refer to variable names at runtime and since the names of
   obfuscated variables are always expected to change between releases, those need to be adjusted.
   Using the wrong name will not allow vane to do what it needs to do.

   To find those in vane, search for `MAGIC VALUE` by `rg "MAGIC VALUE"`. Usually it's a call
   to `MappedRegistry.class.getDeclaredField("l" /* frozen */)` or similar. The comment tells you what clear-text
   variable is called. You can find the new mapped names in `work/minecraft_server.1.19.4.txt`.
   Careful, the file is big (>5MB), so use a editor that can handle this.
   Search for `MappedRegistry`, then look out for `frozen` on the following lines (`boolean frozen -> l`).
   Repeat for all magic values.

   For 1.19.3 -> 1.19.4 we can see that this particular mapping didn't change.

9. Update the resource pack version. As you probably know, vane generates it's own resource pack.
   Resource packs have a version that tells minecraft for which version it was made. A new update
   often includes new possibilites for resource packs and thus we need to make sure it still is
   compatible. Read the changelog to see whether anything crucial (file layout, override rules)
   has changed and ajust the resource pack generator if necessary. Often there are no breaking changes
   and we just need to bump the resource pack version in the mcmeta file. To do that,
   open `ResourcePackGenerator.java` (search for `pack_format`) and enter the [newest value](https://minecraft.fandom.com/wiki/Pack_format).
   You'll see in testing wether that worked or not.

10. Make a commit detailing the update and what issues were encountered if any.
    Don't push yet.

11. Now comes testing. Build one more time `./gradlew build`, and start a testserver
    with the latest paper build and ProtocolLib. Disable resource pack distribution.
	Generate the resource pack with `vane generate_resource_pack`, copy it to your client.

	Enter the server. Now it is important to test those parts of vane which interface with
	the mojang mappings, since those are the most likely to break.

    - `/customitem give vane_trifles:golden_sickle` should display as a sickle, should work when used on wheat. (Tests custom item registration and event dispatching)
    - Take an elytra in your hand, run `/enchant vane_enchantments:angel`, test whether you can accelerate by sneaking. (Tests custom enchantment registration)
    - Duplicate the elytra, go into survival mode (IMPORTANT!) then combine them on an anvil. You should get Angel II.
	- Take a smithing table, combine the elytra with a netherite ingot. (Test's complex smithing recipe integration)
    - Put some random blocks and items in a chest, place a button next to it and press it. The chest should now be sorted.

    Fix issues and make a new commit if necessary.

12. Copy the generated resource pack to `docs/resourcepacks/<new_version>.zip`, and update
	vane's version numer in `build.gradle.kts` (always bump minor version for mojang version updates).
	Commit and add signed tag: `git commit -S -m 'chore: version bump' && git tag -s -m '' v1.11.0`.

13. Push to main: `git push && git push --tags && git switch main && git merge --ff-only develop && git push && git switch develop`.

14. Generate the release artifacts, so definitely run a clean build: `rm -rf target && ./gradlew clean && ./gradlew build`.

15. `./sign_and_zip.sh` (sorry, but you need to be me to get the correct signature.)

16. Draft a new github release, and write a CHANGELOG. Format can be copied from previous releases,
    see `git log` to see what changed. Upload final artifacts to GitHub releases first.
	Afterwards entertain modrinth.com and hangar.papermc.io.

Congratulations, you are now awake and can start implementing new features.
