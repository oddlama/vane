# Module vane-annotations

Declarative annotations that drive vane's configuration, translation, command and content
registration, plus a set of `javax.annotation.processing` validators for their use.

This module has no runtime dependency on the rest of vane. Every other vane plugin depends on it as
`compileOnly` for the annotation types, so it must stay free of `vane-core` imports.

> **Note:** the validators in `org.oddlama.vane.annotation.processor` are not wired into the build.
> They are `javax.annotation.processing` processors, which only run over Java sources or through
> `kapt`; vane is now entirely Kotlin and no longer applies `kapt`, so nothing invokes them. They
> are kept as the reference for what each annotation expects, and as the starting point should the
> checks be ported to KSP. Because they are compile-time-only tooling, `vane-core`'s `shadowJar`
> excludes both the package and its service file from the shipped plugin jar.

## How the pipeline fits together

The annotations here are pure metadata; none of them generate code. They are read at two separate
points in a plugin's life:

1. **At compile time** — historically, by the processors in `org.oddlama.vane.annotation.processor`.
   These only _validate_ — they check that an annotation is on the right kind of element, that the
   annotated class inherits from the framework base type it claims to extend, and that an annotated
   field has exactly the Java type the corresponding runtime field reader expects. See the note
   above: they are currently dormant, so these rules are conventions rather than enforced checks.
2. **At runtime** by `vane-core`. `ConfigManager` scans a module's fields for any annotation whose
   name starts with `org.oddlama.vane.annotation.config.Config` and builds a matching
   `ConfigField` for each; `LangManager` does the same for the `lang` annotations. The `desc`
   parameter carried by most config annotations becomes the comment written above the key in the
   generated `config.yml`, which is why it is mandatory almost everywhere.

Because step 2 is reflective and driven by naming, adding a new `Config*` annotation means touching
three places: the annotation here, an entry in `ConfigAndLangProcessor.fieldTypeMapping`, and a
`ConfigField` implementation plus a `when` branch in `vane-core`'s `ConfigManager`.

## Processor registration

The four processors are registered through
`src/main/resources/META-INF/services/javax.annotation.processing.Processor`. A processor that is
not listed in that file will be silently ignored, so new processors must be added there by hand.

They report the source version as `SourceVersion.latestSupported()` rather than pinning a release,
so moving the toolchain forward does not make javac warn about an outdated processor.

## Build dependencies

This module names exactly one Paper type — `org.bukkit.Material`, used as an annotation member
type by `ConfigMaterial`, `ConfigMaterialSet`, `ConfigMaterialMapEntry`, `ConfigItemStackDef` and
`VaneItem`. It therefore does **not** apply `paperweight`; the root build excludes it and the
module declares a plain `compileOnly(libs.paperApi)` instead, which avoids resolving the Paper dev
bundle and remapping a per-module server jar just to look up an enum. If a future annotation ever
needs a Mojang-mapped or server-internal type, that trade has to be revisited.

# Package org.oddlama.vane.annotation

Contains [org.oddlama.vane.annotation.VaneModule], the single annotation that marks a class as a
vane plugin entry point. Besides the display name and bStats id it carries the three independent
format versions — config, lang and storage — that `vane-core` compares against on-disk files to
decide whether a migration or a regeneration is needed.

# Package org.oddlama.vane.annotation.command

Metadata for command classes. `VaneCommand` is `SOURCE`-retained and exists purely so
`CommandAnnotationProcessor` can enforce that every command carries a `Name`; `Name` and `Aliases`
are `RUNTIME`-retained because the command registration code reads them when the plugin enables.

# Package org.oddlama.vane.annotation.config

One annotation per supported configuration value type. Each generates a key in the module's
`config.yml`, documented by its `desc` and defaulted from its `def`.

Annotations ending in `Entry` (plus `ConfigItemStackDef`) are not applied to fields themselves —
they are nested inside the `def` of a map-valued annotation to express a default map literal, which
is the only way to write nested defaults given Java's annotation constant rules. They declare an
empty `@Target()` to say exactly that: nesting a Kotlin annotation inside another is never target
checked, but an empty target list makes applying one to a field a compile error. That matters
because their names begin with `Config`, and `ConfigManager` builds a `ConfigField` for _every_
field annotation under `org.oddlama.vane.annotation.config.Config*` — without the restriction a
misplaced `@ConfigMaterialMapEntry` would only surface as a "Missing ConfigField handler" crash
when the plugin enables.

The annotated field's name determines the YAML key. `vane-core` strips the mandatory `config`
prefix and uses the remainder verbatim, so `configMinRegionExtentX` becomes `MinRegionExtentX`. The
enclosing `Context` namespace is prepended as a dotted path and rendered as YAML nesting, which is
how `BlueMap.Enabled` and friends appear in the generated file. Renaming a field is therefore a
config-breaking change.

`metrics` controls whether the value is reported to bStats. It defaults to `true` for most types
and to `false` for the free-text and map-shaped ones — `ConfigString`, `ConfigStringList`,
`ConfigDict`, `ConfigStringListMap` and `ConfigMaterialMapMapMap` — since those are the most likely
to hold server-specific information.

# Package org.oddlama.vane.annotation.enchantment

Declares custom enchantments and their generation behaviour. `VaneEnchantment` must be applied to a
subclass of `vane-core`'s `CustomEnchantment`, which `VaneEnchantmentProcessor` enforces.

# Package org.oddlama.vane.annotation.item

Declares custom items. `modelData` reserves a custom model data value that the resource pack
generator in `vane-core` uses to map the item onto its texture, so values must not collide across
modules.

# Package org.oddlama.vane.annotation.lang

Marks fields that are populated from the module's `lang-*.yml` files. Unlike the config annotations
these carry no metadata at all: everything is derived from the field itself. The name must begin
with `lang`, and the remainder becomes the YAML path — `vane-core`'s `LangField` throws at startup
otherwise. The full translation key is that path prefixed with the module namespace. The field's
type (`TranslatedMessage` versus `TranslatedMessageArray`) selects how the value is parsed, which
is exactly what `ConfigAndLangProcessor` describes.

# Package org.oddlama.vane.annotation.persistent

Marks fields that `vane-core`'s persistence layer serializes into the module's `storage.json`.

# Package org.oddlama.vane.annotation.processor

The annotation processors and their shared helpers. They report problems through
`ProcessingEnvironment.messager` with `Diagnostic.Kind.ERROR` and never generate sources. They are
not currently run by the build — see the note at the top of this module.

`ProcessorUtils` holds the two checks every processor needs: that an annotation landed on a class,
and that the class inherits — at any depth — from a required framework base type. The inheritance
check matches on a _prefix_ of the erased supertype name (for example
`org.oddlama.vane.core.module.Module<`) so that it works against the generic base classes vane uses
throughout.

`ClassPlacementProcessor` pairs those two checks into the round loop that every placement-only
processor runs, so `VaneModuleProcessor` and `VaneEnchantmentProcessor` are one line each and
`CommandAnnotationProcessor` only adds its mandatory-`@Name` check on top. `ConfigAndLangProcessor`
stands apart because it validates field types rather than placement; it derives its supported
annotation types from `fieldTypeMapping` so the two can never drift apart.
