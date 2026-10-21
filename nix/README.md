# Nix build and development

The flake is the build/CI interface. Gradle remains the JVM build description and a
fast incremental development tool; contributors do not need any particular VCS.
Use Nix with flakes enabled. Inputs and tool versions are pinned in `flake.lock`,
`toolchain.nix`, the Gradle catalog/wrapper, and `gradle-deps.json`.

## Commands

```sh
nix develop                       # numtide/devshell, JDK 25, Gradle, Python, treefmt
nix build                         # deployable JARs and barmc ZIP in result/
nix build .#bridge                 # runnable barmc and its Python dependencies
nix build .#website                # static site, CSS and every historical resource pack
nix build .#release                # JARs, Paper-only ZIP, bridge ZIP/wheel, checksums, manifest
nix fmt                           # treefmt: Nix, Kotlin, Python, shell, web/Markdown/YAML/JSON
nix flake check -L --keep-going     # all checks for the current host system
```

Nix sources use nixfmt. The devshell sets `TREEFMT_WALK=jujutsu` when the project
root contains `.jj`, keeping formatting inside that workspace even in colocated
checkouts. Other checkouts use `auto`; Git-only contributors and CI do not need jj.
Outside the shell, treefmt's Git walker honors local `.git/info/exclude` patterns:
use those for personal directories rather than tracking personal formatter exclusions.

Linux (x86_64/aarch64) and Apple Silicon macOS are declared; CI runs Linux and macOS.
The pinned nixpkgs has dropped Intel macOS support. Intel Mac and Windows users
can still use Gradle/Python directly; native Windows CI was removed rather
than pretending a Linux Nix/WSL test exercises Windows signal/terminal behavior.

All JVM test classes and bridge test files get **separate named derivations** under
`checks`, automatically discovered from `*Test.kt` and `test_*.py`. Keep those naming
conventions. A test-inventory check compares the source inventory with compiled
classes so a missing exported suite is not silently skipped. The build exports the
actual Gradle `Test.classpath`, including the proxy-core shadow-JAR workaround;
it compiles tests but does not execute them. JUnit 4 then runs each class in its
own sandbox. Loopback network tests are permitted on Darwin; no server is launched.
Documentation, release/server helper tests and formatting are checks too.

```sh
nix build .#checks.x86_64-linux.jvm-core-PersistentStorageTest -L
nix build .#checks.x86_64-linux.bridge-server -L
```

Check outputs contain JUnit text/XML reports. New E2E tests can be added as named
Linux-only `pkgs.testers.runNixOSTest` derivations in `checks.nix`, reusing `plugins`
and the prepatched `paper` package. They will need explicit test-environment EULA
policy, isolated worlds, restart assertions and (where needed) a scripted client.
There is deliberately **no fake successful E2E check**: VM/gameplay tests have not
been implemented or run yet.

## Dependency updates and reproducibility

`plugins.nix` uses nixpkgs' Gradle/MITM dependency capture. Every successful download
is fetched by URL and hash; a local replay proxy serves those bytes during the
sandboxed build. The build cannot fall back to Maven Central, `mavenLocal()`, a
user's Gradle cache or Foojay toolchain downloads. This is why Gradle is **not**
passed `--offline`: it talks to the local replay proxy, not the public network.

After changing dependencies, wrapper/JDK versions or the resolution graph:

1. Pin exact catalog versions; do not restore `+`/`latest` selectors. Transitive
   snapshots/metadata are also frozen by the capture.
2. Run `nix run .#update-gradle-deps` (network-enabled update operation, no publishing).
3. Review `nix/gradle-deps.json` URL/hash changes and commit it with the catalog change.
4. Run `nix build` and `nix flake check -L` in the sandbox.

Only declared JVM/build/pack inputs enter the plugin source; website/CI edits do
not recompile Kotlin. Archive timestamps/order and the Paper-only release ZIP are
normalized, including Paperweight's generated server JAR in the exported test runtimes
(entry contents are preserved). `flake.lock` fixes dependency tooling, too. Dependency mirrors can still
remove old files: cache successful Nix outputs/inputs for long-term availability.
The website's recipe/loot snapshot is documented in `docs/data/README.md`.
Reproducibility does not imply runtime gameplay compatibility.

## Dirty-source development

Inside `nix develop`, `build` runs incremental `gradle build`. Standard tasks such
as `gradle :vane-core:test --tests '*PersistentStorageTest'` work normally, use the
local cache and may access the network. `bridge-test` tests the Python working tree.
`check` is the separate sandboxed validation path; Nix uses tracked dirty files,
so add new source files to version control before a flake build.

```sh
nix run .#testserver -- --help
# After reading and accepting https://aka.ms/MinecraftEULA:
VANE_ACCEPT_EULA=true nix run .#testserver

# Or, in the devshell, build current dirty Kotlin sources incrementally first:
VANE_ACCEPT_EULA=true testserver
```

Both launchers run the same pinned Paper build, install only Paper plugins, and use
`.vane-testserver/` by default. Use `--state-dir PATH` for a separate disposable
instance. The initial properties bind `127.0.0.1:25565`, use online authentication,
and can subsequently be edited there; `--port` only affects a new properties file.
There is no automatic EULA acceptance merely from entering the shell/building.
An unmanaged nonempty directory is rejected, unrelated plugins/worlds are retained,
and changing the Paper runtime requires a fresh state directory. Never use live
worlds. No Velocity/Geyser server is implicitly started.

`paper.nix` fetches hash-pinned Paper and Mojang bytes and runs Paperclip's
**patch-only** mode in the build sandbox. Only this package is allowed under the
flake's unfree-package exception. Building it does not start Minecraft or accept
the EULA. Starting it is an explicit interactive action, outside the build sandbox.
The game/plugins may still use network services at runtime. Client/resource-pack
changes need the usual `/vane generate_resource_pack` and actual client testing.

## Releases

`version.txt` is the suite release version. Create the matching resource-pack ZIP
**before** bumping it. Gradle descriptors/bridge ZIP and the Nix Python wheel use
that version; do not maintain a second Geyser version property.

GoReleaser was considered, but this project has prebuilt JVM/Python artifacts and
needs Modrinth's multipart API rather than a Go compilation pipeline. A small
Nix-built assembler plus `gh`/the official Modrinth API keeps these rules explicit,
without relying on GoReleaser Pro [prebuilt binaries](https://goreleaser.com/customization/prebuilt/)
or hook chains. The old `sign_and_zip.sh` helper is retired; sign actual artifact
files explicitly if needed, outside the reproducible build.

```sh
nix build .#release
nix run .#publish-github              # offline dry-run: verifies hashes, prints plan
nix run .#publish-modrinth            # offline dry-run, including both variants
```

Publishing is separate from building and requires `--confirm`. The source must be
clean/versioned, and the remote `v<version>` tag must identify the exact source
revision in the manifest. `--tag` additionally checks a CI-requested tag matches
the artifacts. Credentials are runtime environment variables, never Nix inputs:

- GitHub uses `GH_TOKEN`/normal `gh` authentication, creates a **draft**, and refuses
  published releases or duplicate asset names. It never silently clobbers assets.
- Modrinth uses `MODRINTH_TOKEN` and `gh` for tag/release verification. Publish the
  reviewed GitHub draft first; its notes are reused. Existing Modrinth versions are
  rejected. There is no transactional upload across variants; after a partial
  failure inspect the project and explicitly retry only `--variant main` or
  `--variant geyser` as appropriate.
- The existing Modrinth layout is preserved: `v<version>` has Paper/Purpur/Velocity
  loaders, core as the primary file, plus `all-plugins.zip` and Velocity;
  `v<version>-Geyser` has the Geyser JAR/loader. Modrinth clients fetching only the
  primary file still install core only; this migration does not change that policy.

Tag CI checks/builds and uploads the GitHub draft. Manual workflow dispatch with
`modrinth: true` publishes the already-reviewed tag to Modrinth. Configure the
GitHub `release` environment with required reviewers and `MODRINTH_TOKEN` as needed.
Nothing signs automatically; maintainer signing remains an explicit external step.
No publisher was exercised with `--confirm` while implementing this pipeline.
