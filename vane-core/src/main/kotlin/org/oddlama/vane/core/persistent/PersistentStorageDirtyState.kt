package org.oddlama.vane.core.persistent

/** Saves run on the owning thread. Mutations made during a save still require a later flush. */
internal class PersistentStorageDirtyState {
    private var revision = 0L
    private var dirty = false

    val isDirty: Boolean
        get() = synchronized(this) { dirty }

    @Synchronized
    fun markDirty() {
        revision++
        dirty = true
    }

    fun save(publish: () -> Boolean): Boolean {
        val savingRevision =
            synchronized(this) {
                // An explicit save also becomes pending if publication fails.
                dirty = true
                revision
            }
        if (!publish()) return false
        synchronized(this) {
            if (revision == savingRevision) dirty = false
        }
        return true
    }
}
