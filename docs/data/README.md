# Explicit website data

`items.json` contains the default recipe/loot examples displayed by the website;
`icons.json` maps item identifiers to checked-in assets under `../assets/`.
They are documentation inputs, not server configuration or a new gameplay registry.

The initial snapshot was migrated from the previously published `docs/index.html`
at `f69ceb70`. It preserves those examples rather than claiming they were freshly
exported from a running server. The `itemlike` identifiers retain their historical
spelling so existing content and anchors do not change during the build migration.

Review these examples alongside gameplay/recipe changes. Actual recipes and loot
remain configurable in Kotlin/server configuration. Do not point a website build
at a production server's configuration: that is neither reproducible nor safe to
publish. All referenced icons and local links are validated during the build.

`nix build .#website` renders Markdown/templates, compiles CSS using the pinned
Tailwind package and adds the historical resource packs. No client JAR, npm install,
server process, network access or private configuration is needed during the build.
