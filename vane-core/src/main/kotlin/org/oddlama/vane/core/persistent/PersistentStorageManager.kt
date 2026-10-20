package org.oddlama.vane.core.persistent

import org.json.JSONObject
import org.oddlama.vane.core.module.Module
import java.io.File
import java.util.function.Consumer

/** Plugin-facing bindings for the filesystem storage engine. */
class PersistentStorageManager(var module: Module<*>) {
    /** A migration that upgrades storage to [to]. */
    class Migration(val to: Long, val name: String?, val migrator: Consumer<JSONObject?>)

    private val storage = PersistentStorage(
        { module.annotation.storageVersion },
        { module.storagePathOf("storageVersion") },
        { module.log },
    ).also { it.compile(module) { name -> name } }

    /** Whether persistent data has been loaded successfully. */
    var isLoaded: Boolean
        get() = storage.isLoaded
        set(value) { storage.isLoaded = value }

    /** Discovers persistent fields on a module or component. */
    fun compile(owner: Any, mapName: (String?) -> String?) = storage.compile(owner, mapName)

    fun addMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) =
        storage.addMigrationTo(to, name, migrator)

    fun load(file: File): Boolean = storage.load(file)

    /** Keeps the existing JVM void signature for callers that do not need an outcome. */
    fun save(file: File) = storage.save(file)

    /** Reports publication failure so callers can retain pending changes for retry. */
    fun trySave(file: File): Boolean = storage.trySave(file)
}
