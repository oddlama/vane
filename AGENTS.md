# Working on vane

Vane is a modular, vanilla-friendly Minecraft plugin suite. Preserve its immersive, configurable gameplay and
compatibility with existing servers. Prefer focused changes over unrelated rewrites or new frameworks.

This file contains shared project guidance, not personal editor, agent, or version-control preferences.

## Start here

- Read the affected module's `Module.md`, its build script, and nearby implementations/tests before editing.
  `Module.md` files also supply the module/package introductions for Dokka.
- `CONTRIBUTING.md` describes contribution expectations; use Conventional Commits for commit messages
  (for example, `fix(core): preserve pending storage changes`). CI checks PR commit messages.
- Use the build scripts, `version.txt`, `nix/README.md` and source as the authority for versions and behavior.
  Develop against the repository's current default branch; do not infer a branch from historical links.
- Read `UPDATING.md` for Minecraft/platform upgrades and `contrib/maintenance/persistence-safety.md`
  before changing storage, migration, or world-loading behavior. The latter documents both guarantees and limits.
- `TODO` is an ideas list, not a specification or authorization to expand the current task.

## Repository map

There are 14 Gradle subprojects, included in `settings.gradle.kts`. JVM sources are Kotlin under
`src/main/kotlin`; tests live in `src/test/kotlin`, and bundled resources in `src/main/resources`.
Most packages start with `org.oddlama.vane`; Velocity deliberately uses `org.oddlama.velocity`.

| Module                  | Responsibility                                                                                   |
| ----------------------- | ------------------------------------------------------------------------------------------------ |
| `vane-annotations`      | Config, language, storage, command, item and enchantment annotations; legacy validators.         |
| `vane-core`             | Paper plugin/framework: lifecycle, config, translations, storage, commands, menus, items, packs. |
| `vane-admin`            | Administration commands, autostop, chat formatting and world/spawn/hazard protection.            |
| `vane-bedtime`          | Per-world sleep thresholds, night skipping and map indicators.                                   |
| `vane-busybar`          | Paper events, presence and authenticated streams for BUSY Bar; optional module hooks.            |
| `vane-busybar-common`   | Platform-independent HTTP/TLS, pairing, heads and backend-link protocol.                         |
| `vane-enchantments`     | Custom enchantments, registry/bootstrap definitions and ancient tomes.                           |
| `vane-permissions`      | Permission groups/inheritance, attachments and vouching.                                         |
| `vane-portals`          | Portal construction, transport, styles, events, menus and world persistence.                     |
| `vane-regions`          | Claims, groups, roles, protection enforcement, economy/portal/map integrations.                  |
| `vane-trifles`          | Independent quality-of-life features and custom items, including portable storage.               |
| `vane-proxy-core`       | Platform-neutral proxy features, config, maintenance, managed servers and BUSY Bar.              |
| `vane-velocity`         | Velocity entry point, adapters and packaging of proxy-core/common code.                          |
| `vane-geyser-extension` | Geyser extension: Bedrock items, command forms, generated Bedrock pack/locales.                  |

Other important areas:

- `vane-busybar/bridge/`: separate Python package `barmc`, with its own README, dependencies and pytest suite.
- `docs/`: website sources, TOML catalog, Markdown features, templates, Python generator and Tailwind setup.
- `docs/assets/`, `docs/images/`, `docs/wiki/` and `docs/data/`: checked-in assets and recipe/loot examples.
- `docs/resourcepacks/`: published versioned resource-pack ZIPs; compatibility-sensitive build inputs.
- `contrib/assets/`: artwork/source assets (including XCF files); `contrib/maintenance/`: maintenance notes.
- `.github/workflows/`: JVM/bridge builds, commit-message linting, Pages deployment and releases.
- `flake.nix`, `flake.lock`, `nix/`: flake-parts build/check/release pipeline, devshell and treefmt; `.envrc` for direnv.
- `gradle/libs.versions.toml`: dependency/plugin version catalog; `gradle/wrapper/`: Gradle wrapper.

`vane-annotations`, `vane-busybar-common` and `vane-proxy-core` are libraries, not independently installed plugins.
Velocity belongs on the proxy and the Geyser extension belongs in Geyser, not Paper's plugin directory.

## Build and validation

Nix is the canonical build/CI interface; see `nix/README.md` for architecture and update procedures.
The JVM toolchain is **JDK 25**; the flake, catalog and wrapper pin tool/dependency versions.

```sh
nix develop
nix build                         # deployable artifacts in result/
nix build .#website
nix build .#release
nix fmt
nix flake check -L --keep-going    # formatting, JVM, bridge, tooling, docs and release checks
```

Each `*Test.kt` class and bridge `test_*.py` file is a separate flake check. Follow these naming conventions;
the test-inventory check compares discovered sources with exported classes. JVM tests use JUnit 4/`kotlin.test`,
not JUnit 5. Nix compiles/export runtimes once, then executes each suite in an independent sandbox.

For fast incremental work inside the shell, use `gradle build`, `gradle :vane-core:test --tests '*PersistentStorageTest'`
or `bridge-test`. The wrapper (`./gradlew`, Windows `gradlew.bat`) and a Python virtualenv with `.[test]` remain
available without Nix. Local Gradle/Python workflows may access the network; they are not sandboxed Nix validation.
Gradle's build/configuration caches remain enabled for development. The Nix build uses the pinned toolchain and
locked dependency replay, not a user cache. Update dependencies via `nix run .#update-gradle-deps`, review the
URL/hash diff in `nix/gradle-deps.json`, then validate in the sandbox. Do not reintroduce floating catalog versions.

Deployable Gradle artifacts are copied to `target/`; the root deliberately produces no `vane.jar`.
Do not deploy arbitrary plain `build/libs/` JARs. API documentation remains available through
`gradle :dokkaGeneratePublicationHtml`. Test affected consumers when changing shared modules.
Gradle packages the bridge ZIP but does not run its Python tests; `nix flake check` does.
Keep Python's terminal/signal handling portable; native Windows CI is not provided by Nix.
See `vane-busybar/bridge/README.md` for component responsibilities, previews and hardware-testing limits.

### Runtime validation

**Ask before starting services.** `nix run .#testserver` uses immutable artifacts; the devshell's `testserver`
builds dirty sources first. Both require `VANE_ACCEPT_EULA=true` explicitly and default to a disposable,
loopback-only `.vane-testserver/`. Building/prepatching Paper does not accept the EULA or launch Minecraft.
Legacy `gradle runServer`/`runVelocity` remain interactive alternatives; automatic EULA acceptance was removed.

- Never test against a live server's only data. Preserve backups; runtime cleanup tasks are destructive.
- Pure unit tests do not replace startup/shutdown, config reload, permissions, cancellation, optional-plugin,
  client/resource-pack and actual process-restart checks.
- Follow `contrib/maintenance/persistence-safety.md` for storage/world lifecycle changes and deployment gates.
- Future E2E VM checks belong in `nix/checks.nix`; none are implemented merely by exposing a test-server app.
- State exactly what was tested. A successful build is not proof of runtime compatibility.

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

- `nix fmt` runs treefmt (nixfmt, ktfmt, Ruff format, shfmt and Prettier); `nix flake check` enforces it.
  Compatibility assets, generated dependency captures and runtime/build directories are excluded.
  Avoid unrelated semantic cleanups while formatting. Do not check in local hook configurations.
- Keep API/module explanations in KDoc/`Module.md`; website text in `docs/content/`, registered in
  `docs/content.toml`; templates/source CSS in `docs/templates/`.
- `nix build .#website` uses checked-in assets and explicit documented examples, not a client JAR/live server.
  Review `docs/data/` with recipe/loot changes. The snapshot is documentation, not a gameplay registry.
  Generated HTML/CSS are not tracked. Pages deploys the Nix-built output with all historical resource packs.
- `version.txt` is the suite release version. Add the matching `docs/resourcepacks/v<version>.zip` before bumping it.
  Descriptors and bridge distributions receive that version during packaging; there is no duplicate Geyser property.
- `nix build .#release` assembles the Paper-only ZIP, individual JARs, bridge ZIP/wheel, checksums and provenance.
  Velocity/Geyser never belong in `all-plugins.zip`.
- Publishers default to offline dry-runs. Explicit confirmation, clean source/tag verification and runtime credentials
  are required. GitHub uploads drafts only and never silently replaces assets; Modrinth follows a reviewed published
  GitHub release. See `nix/README.md` and `UPDATING.md`. CI's release environment should require reviewer approval.
- Do not bump versions, overwrite published packs, sign, tag, start servers or publish as a side effect of an ordinary
  fix. Keep worlds/configs, caches, credentials and personal tooling out of shared changes.
