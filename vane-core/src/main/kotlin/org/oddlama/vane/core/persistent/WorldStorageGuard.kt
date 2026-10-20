package org.oddlama.vane.core.persistent

import java.util.UUID

/**
 * Owning-thread write barrier for world storage. An absent in-memory record only means deletion
 * after the complete world was read and indexed successfully. Unseen, loading and failed worlds
 * must retain all stored bytes, including records whose type or schema we could not read.
 */
class WorldStorageGuard {
    private val loaded = mutableSetOf<UUID>()
    private val loading = mutableMapOf<UUID, Any>()

    fun canSave(worldId: UUID): Boolean = worldId in loaded

    /** A failed reload revokes a previous successful load as well. Exceptions reach the caller. */
    fun load(worldId: UUID, readAndIndex: () -> Unit) {
        loaded.remove(worldId)
        val attempt = Any()
        check(loading.putIfAbsent(worldId, attempt) == null) { "World storage is already loading: $worldId" }
        try {
            readAndIndex()
            // An unload/disable during the callback must not be undone by a late completion.
            if (loading[worldId] === attempt) loaded.add(worldId)
        } finally {
            loading.remove(worldId, attempt)
        }
    }

    /** Gates every mutation, not only deletion, to avoid overwriting a failed record by its ID. */
    fun save(worldId: UUID, publish: () -> Unit): Boolean {
        if (!canSave(worldId)) return false
        publish()
        return true
    }

    fun forget(worldId: UUID) {
        loaded.remove(worldId)
        loading.remove(worldId)
    }

    fun clear() {
        loaded.clear()
        loading.clear()
    }
}
