# Working on vane

Vane is a modular, vanilla-friendly Minecraft plugin suite. Preserve its immersive, configurable gameplay and
compatibility with existing servers. Prefer focused changes over unrelated rewrites or new frameworks.

This file contains shared project guidance, not personal editor, agent, or version-control preferences.

## Start here

- Read the affected module's `Module.md`, its build script, and nearby implementations/tests before editing.
  `Module.md` files also supply the module/package introductions for Dokka.
- `CONTRIBUTING.md` describes contribution expectations; use Conventional Commits for commit messages
  (for example, `fix(core): preserve pending storage changes`). CI checks PR commit messages.
- Use the current build scripts and source as the authority for versions, dependencies, and behavior.
  Some prose is historical: `CONTRIBUTING.md` mentions JDK 17 and `develop`, while `UPDATING.md`, source links,
  and the Pages workflow still refer to `Kotlin`. Do not switch branches based only on those references.
- Read `UPDATING.md` for Minecraft/platform upgrades and `contrib/maintenance/persistence-safety.md`
  before changing storage, migration, or world-loading behavior. The latter documents both guarantees and limits.
- `TODO` is an ideas list, not a specification or authorization to expand the current task.

## Repository map

There are 14 Gradle subprojects, included in `settings.gradle.kts`. JVM sources are Kotlin under
`src/main/kotlin`; tests live in `src/test/kotlin`, and bundled resources in `src/main/resources`.
Most packages start with `org.oddlama.vane`; Velocity deliberately uses `org.oddlama.velocity`.

| Module | Responsibility |
| --- | --- |
| `vane-annotations` | Config, language, storage, command, item and enchantment annotations; legacy validators. |
| `vane-core` | Paper plugin/framework: lifecycle, config, translations, storage, commands, menus, items, packs. |
| `vane-admin` | Administration commands, autostop, chat formatting and world/spawn/hazard protection. |
| `vane-bedtime` | Per-world sleep thresholds, night skipping and map indicators. |
| `vane-busybar` | Paper events, presence and authenticated streams for BUSY Bar; optional module hooks. |
| `vane-busybar-common` | Platform-independent HTTP/TLS, pairing, heads and backend-link protocol. |
| `vane-enchantments` | Custom enchantments, registry/bootstrap definitions and ancient tomes. |
| `vane-permissions` | Permission groups/inheritance, attachments and vouching. |
| `vane-portals` | Portal construction, transport, styles, events, menus and world persistence. |
| `vane-regions` | Claims, groups, roles, protection enforcement, economy/portal/map integrations. |
| `vane-trifles` | Independent quality-of-life features and custom items, including portable storage. |
| `vane-proxy-core` | Platform-neutral proxy features, config, maintenance, managed servers and BUSY Bar. |
| `vane-velocity` | Velocity entry point, adapters and packaging of proxy-core/common code. |
| `vane-geyser-extension` | Geyser extension: Bedrock items, command forms, generated Bedrock pack/locales. |

Other important areas:

- `vane-busybar/bridge/`: separate Python package `barmc`, with its own README, dependencies and pytest suite.
- `documentation/`: website sources, TOML catalog, Markdown features, templates, Python generator and Tailwind setup.
- `docs/`: published static website, images and versioned resource-pack ZIPs; not just disposable build output.
- `contrib/assets/`: artwork/source assets (including XCF files); `contrib/maintenance/`: maintenance notes.
- `.github/workflows/`: JVM/bridge builds, commit-message linting, Pages deployment and releases.
- `flake.nix`, `flake.lock`, `.envrc`: optional Nix/direnv development environment and Nix checks.
- `gradle/libs.versions.toml`: dependency/plugin version catalog; `gradle/wrapper/`: Gradle wrapper.

`vane-annotations`, `vane-busybar-common` and `vane-proxy-core` are libraries, not independently installed plugins.
Velocity belongs on the proxy and the Geyser extension belongs in Geyser, not Paper's plugin directory.

## Build and validation

Run Gradle commands from the repository root using `./gradlew` (`gradlew.bat` on Windows).

- The Kotlin JVM toolchain is **JDK 25**, configured centrally in `build.gradle.kts`; CI also uses JDK 25.
  Kotlin's plugin version is in `settings.gradle.kts`, Gradle's in `gradle/wrapper/gradle-wrapper.properties`.
- `nix develop` is optional. It provides Java/Gradle, Node, Python documentation dependencies and Nix tools.
  The pinned shell's `temurin-bin` need not be JDK 25: distinguish the JVM launching Gradle from the requested
  compilation toolchain. The Foojay resolver can provision toolchains; first builds need network/cache access.
- Paper and paperweight currently use dynamic versions in the catalog. Resolution/download failures are not
  necessarily source failures; report the actual error rather than changing versions or lowering the toolchain.

```sh
# Full JVM build, tests and distributable packaging
./gradlew build --no-daemon --warning-mode all

# All JVM tests, or a focused module/test class
./gradlew test
./gradlew :vane-core:test
./gradlew :vane-core:test --tests '*PersistentStorageTest'

# Example focused plugin build
./gradlew :vane-portals:build

# Aggregated API documentation (build/dokka/html)
./gradlew :dokkaGeneratePublicationHtml
```

- Deployable JARs are copied to root `target/`. Do not deploy an arbitrary plain JAR from a module's `build/libs/`.
  The root project intentionally produces no `vane.jar`.
- A focused module build compiles needed dependencies but does not imply their test suites ran. Test affected
  consumers too when changing `vane-core`, proxy-core or busybar-common; use the full build for cross-module changes.
- Gradle's parallel execution, build cache and configuration cache are enabled. For troubleshooting, the persistence
  maintenance guide uses `--no-configuration-cache --max-workers=2`; do not disable caches project-wide as a workaround.
- JVM tests use `kotlin.test` with JUnit 4 support, including `TemporaryFolder`. Follow existing tests rather than
  assuming JUnit 5/MockBukkit is configured. Tests cover storage failure/retry, legacy keys, item conversion/durability,
  resource-pack identity, menus, portal API compatibility, proxy config and BUSY Bar protocols.
- Add regression tests near the changed behavior. Pure helpers and injected filesystem/network boundaries allow
  testing without booting Paper. JVM reports are under each module's `build/reports/tests/test/`.

### Python bridge

Gradle packages the bridge ZIP but does **not** execute its Python tests. From `vane-busybar/bridge/`:

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -e '.[test]'
.venv/bin/python -m pytest
```

Use `.venv\Scripts\python.exe` on Windows. The package requires Python 3.10+; CI currently tests Python 3.15
on Linux, macOS and Windows. Keep platform-specific terminal/signal handling portable.
`barmc/state.py` is display-independent state; `server.py` handles events/actions; `pairing.py` handles TLS pinning;
`display/` holds device and terminal adapters. See its README for `--terminal` previews and hardware-testing limits.

### Runtime validation

`./gradlew runServer` starts the Paper development server with plugins under `run/`;
`./gradlew runVelocity` delegates to the proxy module's development task.
**Ask before starting these services.** `runServer` explicitly passes `-Dcom.mojang.eula.agree=true`, opens a
server and creates runtime data; compilation/unit tests do not require accepting the EULA or starting a server.

- Use disposable worlds or coherent copies of backups, never a live server's only data.
- `cleanVaneRuntimeTranslations`, `cleanVaneConfigurations`, `cleanVaneStorage`, `cleanVane` and `cleanWorld`
  target runtime data. Do not run them casually to make a failure disappear.
- For gameplay/platform changes, test enable/disable/config reload, permissions, cancelled events, optional plugins
  both present and absent, and the affected mechanics. Custom items/enchantments also need client/resource-pack checks.
- Persistence changes need actual process restarts and world lifecycle tests beyond unit tests; follow the deployment
  gate in `contrib/maintenance/persistence-safety.md`. A fresh in-process storage engine is not a server restart.
- State exactly what was tested and what remains unverified. Do not equate a successful build with
  runtime compatibility.

## Architecture and compatibility rules

### Modules, components and optional integrations

- Paper entry points extend `Module<T>` and carry `@VaneModule`. The framework is in
  `vane-core/src/main/kotlin/org/oddlama/vane/core/module/`.
- `Context`/`ModuleContext` define config/language namespaces. `ModuleGroup` adds an `Enabled` flag.
  Features usually register by constructing components in the module's `init` block; prefer this pattern over a
  parallel lifecycle system. `vane-core`'s `Listener` component handles listener registration/unregistration.
- Use framework scheduling/lifecycle hooks and clean up tasks/listeners/resources when disabled. Bukkit/world access
  belongs on the owning server thread; a synchronized dirty flag does not make serialization or Bukkit objects safe
  to access asynchronously. Network work must hand game-state mutations back to the server thread.
- Keep optional dependency classes behind checked hooks/delegates, as in portals/regions map layers and busybar's
  `hooks/`. Update dependency descriptors when needed; an absent optional plugin must not cause eager class loading.
- Proxy features normally belong in `vane-proxy-core` against its abstractions; Velocity-specific APIs belong in
  `vane-velocity`. Keep Paper/core dependencies out of proxy-core and busybar-common. Geyser targets its own API.
- Preserve public JVM APIs as well as Kotlin source compatibility: getters, `@JvmField`, `@JvmStatic`, constructors,
  Bukkit static `getHandlerList()` methods and return types can all matter to external plugins. Consult
  `EventApiCompatibilityTest` and persistence signature tests before simplifying interop declarations.

### Reflective configuration and translations

- Annotated fields are discovered through reflection. Their names are part of the on-disk contract:
  strip `config`, `lang` or `storage`, then prepend the context namespace. For example, `configMinRegionExtentX`
  contributes `MinRegionExtentX`, not a freely renameable implementation detail.
- Preserve annotation targets, backing fields and reflective overrides such as `configFooDef()`,
  `configFooDesc()` and `configFooMetrics()`. Runtime population explains many initially null fields.
- Config annotation descriptions/defaults generate `config.yml`. Change those sources, not generated runtime configs.
  New config types require the annotation, a core `ConfigField` implementation and `ConfigManager.compileField` mapping;
  keep `ConfigAndLangProcessor.fieldTypeMapping` consistent too.
- The `javax.annotation.processing` validators in `vane-annotations` are **not wired into this all-Kotlin build**
  (no kapt/KSP). Their presence does not mean annotation correctness is checked at compile time.
- Update bundled `lang-*.yml` key sets when adding/removing language fields. Preserve positional format arguments,
  color formatting and `ResourcePackLangCode`; do not assume missing translations fall back to English.
  Keep file `Version` and `@VaneModule.langVersion` consistent; embedded language updates depend on those versions.
- Config, language and storage versions on `@VaneModule` are separate contracts, not the release version.
  Decide explicitly whether each needs a bump/migration for the change; never relabel incompatible data as compatible.
- `LegacyKeys` supports older snake_case config/storage. Preserve known aliases and backups, but do not indiscriminately
  normalize administrator-defined recipe/loot/server identifiers. See `ConfigEntryNamesTest` and proxy migration tests.

### Persistence: fail closed

Module JSON storage and portal/region world persistent data containers (PDC) are separate persistence mechanisms.
Read the maintenance guide and existing tests before touching either.

- Reject unsupported, malformed or conflicting version aliases before assignment, backup or migration. Failed loads
  revoke write permission; retrying with a missing file must not turn rejected storage into empty defaults.
- Run only pending migrations through the supported version. Keep legacy backups and original data intact on failure.
- Module saves must serialize completely, write a temporary file and atomically replace the old file before
  acknowledging dirty state. Preserve retry state after serialization/write/replacement failure and mutations during
  a save. Do not add a non-atomic replacement fallback or silently publish a partial document.
- Call `markPersistentStorageDirty()` for module storage mutations. Portal/region models also have their own dirty
  flags/setters; direct mutations must not bypass invalidation.
- Every world-PDC mutation, including deletion cleanup, requires a complete successful world load and valid module
  storage. Preserve `WorldStorageGuard`, record/UUID/ownership validation and unload/disable revocation.
- A rejected world load requests shutdown rather than running with a partial protection/portal index. Never delete
  records, lower schema versions or skip validation just to get a server to start.
- Atomic JSON replacement is not a transaction across world saves, PDC, inventories and module files, nor an fsync
  guarantee. Do not claim stronger durability or migration qualification than the tests establish.

### Items, packs and platform internals

- Custom items use `@VaneItem` and the core registry; namespaced IDs, model-data IDs, item versions and PDC keys
  are persistent identities. Keep model data unique across the suite and preserve existing identities/converters.
- Adding an item usually touches registration, recipes/loot, translations, resource-pack assets and Geyser's
  `ItemRegistration.kt`. Enchantments have both behavior and registry definitions plus bootstrap registration.
- Module pack assets live under `src/main/resources/resource_pack/`; update the explicit `resource_pack/index`
  when adding files. A file merely present in that directory is not automatically included by runtime generation.
- `/vane generate_resource_pack` generates the Java pack on a running server. Release packs are checked in at
  `docs/resourcepacks/v<version>.zip`. Core computes the published pack's SHA-1 during the build; Geyser converts the
  same ZIP into a deterministic Bedrock pack/locales. A missing matching ZIP breaks builds, not just distribution.
- Edit generator/source assets rather than `build/generated/` outputs. `Version.kt` in core and proxy-core,
  expanded plugin descriptors and the Bedrock pack are generated by Gradle.
- Treat published pack ZIPs, pack UUIDs and the head library as compatibility-sensitive assets. Head-library texture
  reconstruction must stay byte-identical for existing skull matching; see `vane-core/Module.md`.
- Paper/NMS/CraftBukkit calls and reflective field access can break on upgrades even when code compiles.
  Follow `UPDATING.md`, check plugin `api-version` declarations and `resource_pack/pack.mcmeta`, and test the actual
  packaged server/client. Use the Paper dev bundle rather than reviving historical BuildTools/remapping workflows.

### Packaging and BUSY Bar security

- Shading rules are largely centralized in root `build.gradle.kts`. Core supplies relocated Kotlin/JSON to most
  Paper plugins. Enchantments deliberately retains its own Kotlin runtime because bootstrapping happens before the
  normal plugin dependency class-loader group exists. Preserve this distinction.
- Velocity and Geyser have independent shaded/minimized runtimes. Server-provided dependencies and excluded transitive
  dependencies should not be casually bundled back in. After shading changes, validate packaged artifacts,
  not only tests.
- BUSY Bar changes span Paper, busybar-common, proxy-core and the Python bridge. Keep event/action schemas, audiences,
  permission filtering, snapshots and backend-switch behavior in sync; consult `vane-busybar/Module.md`.
- Preserve token hashing, TLS public-key pinning before sending credentials, and backend nonce/HMAC authentication.
  Do not weaken checks, log pairing secrets or enable unrequested plaintext fallbacks to simplify testing.

## Documentation, formatting and releases

- Follow `.editorconfig` and nearby style: UTF-8/LF, generally four spaces and 120 columns; YAML/JSON/shell use
  two-space indentation. Avoid repository-wide reformatting or speculative nullability/naming cleanups.
- No Gradle ktlint/detekt/Spotless task is configured. `.clang-format` and the Java Prettier configuration do not
  establish a Kotlin formatter command. Nix hooks currently run Alejandra and Statix; `nix fmt` formats Nix.
  The generated `.pre-commit-config.yaml` is ignored, not a file to check in.
- Keep API/module explanations in KDoc/`Module.md`; keep website feature text under `documentation/content/`
  and list new pages in `documentation/content.toml`. Templates and source CSS live in `documentation/templates/`.
- Website rebuild entry point: from `documentation/`, install Node dependencies with `npm install`, supply
  `minecraft-client.jar` and a fresh server's `plugins/` directory, then run `./build.sh`. Python needs `markdown`,
  `toml` and `PyYAML`; block rendering also invokes ImageMagick's `convert` (not supplied by the current flake).
  The generator still contains legacy config-key/asset-path assumptions, so verify compatibility before regenerating;
  do not promise a clean-checkout website build or treat a tooling mismatch as permission to rewrite server data.
- `docs/index.html` and `docs/css/style.css` are generated/published output; prefer editing their sources and review
  generated diffs. Pages deploys `docs/` as-is, including resource packs; it does not run the website generator.
- CI builds the deployable JVM modules and tests the Python bridge separately. Release tags `v*` trigger
  `.github/workflows/release.yml`, which checks the root version, builds JARs/bridge distributions and uploads assets.
  New releases are drafts; rerunning for an existing release can replace its assets.
- For requested release work, follow `UPDATING.md` and the workflow: generate/copy the pack for the new version,
  synchronize root version and `vane-geyser-extension/gradle.properties`, then build before tagging.
  Bridge packaging injects the release version. `all-plugins.zip` is for Paper plugins only, not Velocity/Geyser.
  `sign_and_zip.sh` is a separate maintainer signing helper, not a prerequisite for building or testing.
- Do not bump versions, overwrite published packs, sign, tag or publish as a side effect of an ordinary fix.
  Keep runtime worlds/configs, caches, build artifacts, credentials and personal tooling out of shared changes.
