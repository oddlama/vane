# Persistence safety changes

This focused change is based on upstream `a6f5e2ea` (the reviewed `v1.23.0`). It does
not change storage keys, item identities, schema versions, platform dependencies,
portal transport, visuals, or release packaging. It is not a complete migration
qualification and must not be used to overwrite the published `v1.23.0` artifacts.

## Behavior

### Module JSON storage

- Reject a future, negative, invalid or conflicting version before field
  assignment, legacy-key backup or migration. Check both current and recognized
  legacy version spellings; the current key cannot conceal a future legacy value.
- Revoke write permission on every failed load. Repeated attempts to load a
  missing file cannot turn a rejected/reloaded store into fresh defaults. First
  initialization with no file still works. A subsequent valid file can recover.
- Run only pending migrations up to the supported version. Existing legacy-key
  backups and supported migrations remain in place.
- Stop module initialization after storage rejection while the existing shutdown
  request is pending.
- Preserve the public void `save(File)` and `savePersistentStorage()` signatures.
  The manager's additive `trySave(File): Boolean` reports publication success.
- Acknowledge pending changes only after complete serialization, temporary-file
  writing and atomic replacement succeed. Failure retains retry state; a change
  marked during saving also remains pending. Periodic and shutdown saves use the
  same path. There is no non-atomic replacement fallback.

`PersistentStorage` separates filesystem operations from the plugin lifecycle for
fault-injection tests. `PersistentStorageDirtyState` tracks acknowledgement; its
synchronization does **not** make Bukkit objects or their serialization thread-safe.

### Portal and region world PDC

Every write, including deletion cleanup, requires a successful complete load of
that world and successfully loaded module JSON storage. The shared
`WorldStorageGuard` records this permission by world UUID on the owning thread.

The snapshot reader rejects malformed/noncanonical UUID keys, missing or
wrong-typed values, malformed JSON and decoder failures. The modules also check
record ID/primary-world ownership and reject an ID already indexed in another
world. All PDC records are decoded before indexing. Legacy entries are retired
only after successful indexing and only when actually read from that world's PDC,
not merely found in an old runtime index.

A failed world load logs the affected world/record, retains write protection and
**requests server shutdown**, rather than silently continuing with a partial
portal/protection index. This intentionally makes corrupt data an operational
failure; do not delete offending records just to make the server start.

- Unseen/failed worlds cannot overwrite or clean up their stored records.
- Healthy worlds remain independently writable; intentional deletions still work.
- Non-cancelled world unload and module disable revoke load permission, including
  exceptional cleanup paths. A later load rebuilds that world's runtime indices
  so region lookup lists do not accumulate duplicate entries.
- Duplicate startup/world-load notifications are ignored after successful load.
- Regions, like portals, initialize worlds already loaded before module enable.
  Deferred initialization checks that the module is still enabled.

No PDC schema/key migration or record-layout rewrite is introduced here.

## Automated validation

Run with JDK 25:

```sh
./gradlew --no-daemon --console=plain --no-configuration-cache \
  --max-workers=2 clean build
```

Local validation used JDK **25.0.4+7**, Gradle **9.8.2**, Paper
**26.3.build.207-stable** and Paperweight **2.0.0-beta.24**, with `--offline` against
the previously populated review dependency cache and `--no-build-cache` for the
clean build. This is not a new dependency lock or sandboxed Nix build.

**131 JVM tests passed**, including these **40 new tests** in `vane-core`:

| Test class | Tests | Scope |
| --- | ---: | --- |
| `PersistentStorageTest` | 12 | Real files: schema rejection, alias conflicts, repeated failed reloads, malformed input, supported migration/backup, new stores and fresh-engine reads |
| `PersistentSaveTest` | 8 | Real files: serializer/write/replacement failures and retry; dirty acknowledgement, changes during save, throwing callbacks and JVM save signatures |
| `WorldStorageGuardTest` | 13 | Production guard: skipped/partial loads, no retry of failed worlds, deletion, independent worlds, reload, unload/disable and late completion |
| `PersistentWorldRecordsTest` | 7 | Production snapshot reader with a read-only PDC double: nonempty and empty snapshots, UTF-8 records, unrelated keys and rejected record cases |

The existing **10 external compatibility probes** also passed. They remain
external review evidence, not ten additional maintained tests in this branch.

## Limits and deployment gate

These tests do not boot Paper, emulate CraftWorld, load actual historical portal
or region worlds, or execute Bukkit lifecycle callbacks. The file round trip uses
a fresh engine, **not a process restart**. Server shutdown timing, world
unload/re-enable behavior, indexing/map-provider side effects and real PDC disk
publication still need packaged-server tests.

This is not a cross-file transaction, a journal, a last-good-state reload system,
or a power-loss/fsync guarantee. Field assignment and indexing can still partially
mutate memory before failure; rejected storage must not be used to continue play.
An atomic module JSON rename does not make world PDC, world saves, inventories and
module JSON atomic together. Unknown fields in otherwise supported schemas are
not generally preserved by the existing serializer.

Before deployment, on coherent **copies** of backed-up worlds:

1. Load nonempty historical portals/styles, regions/groups/roles and items. Save
   and verify through two actual process restarts.
2. Inject module serialization/write/replacement failures; verify old file bytes,
   pending retry, recovery, and a successful subsequent save.
3. Test corrupt JSON/enum/type/key/ID/world records, an unavailable world, a save
   before loading, cancelled and successful unloads, and disable/re-enable. Failed
   world records must remain byte-identical even during shutdown cleanup.
4. Test future/conflicting schemas and a missing file on repeated reloads. Verify
   no replacement or new legacy backup, then restore valid input and restart.
5. Verify legacy entries are retired only after a real world save/restart has
   established the new PDC record, and that healthy-world deletion still works.

If startup refuses data: stop the server, preserve a coherent backup and the
original bytes, inspect the logged version/key/world, and repair a copy or use the
matching supported build. Never lower the version label or remove a region to
bypass the guard. No live Minecraft test or EULA acceptance was performed for this
branch.
