package org.oddlama.vane.core.persistent

import org.json.JSONObject
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.oddlama.vane.annotation.persistent.Persistent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    private class Fields {
        @Persistent var storageVersion = 1L
        @Persistent var storageValue = "initial"
    }

    private val fields = Fields()
    private val log = Logger.getAnonymousLogger().apply { level = Level.OFF }
    private fun storage(version: Long = 1) = PersistentStorage({ version }, { "Version" }, { log }).also {
        it.compile(fields) { name -> name }
    }

    @Test
    fun `future schemas are rejected before field mutation migration or any write`() {
        for (key in listOf("Version", "version")) {
            val folder = temporary.newFolder()
            val file = folder.resolve("storage.json")
            val original = """{"$key":"99","Value":"must not load","Unknown":{"keep":true}}"""
            file.writeText(original)
            val store = storage()
            var migrated = false
            store.addMigrationTo(1, "must not run") { migrated = true }

            assertFalse(store.load(file))
            assertFalse(store.isLoaded)
            assertFalse(migrated)
            assertEquals("initial", fields.storageValue)
            store.save(file)
            assertEquals(original, file.readText())
            assertEquals(listOf("storage.json"), folder.list()!!.toList())
        }
    }

    @Test
    fun `current key does not conceal a future or conflicting legacy version`() {
        for (legacy in listOf("99", "0")) {
            val file = temporary.newFile()
            val original = """{"Version":"1","version":"$legacy","Value":"must not load"}"""
            file.writeText(original)
            val store = storage()
            assertFalse(store.load(file))
            store.save(file)
            assertEquals(original, file.readText())
            assertEquals("initial", fields.storageValue)
        }
    }

    @Test
    fun `invalid versions do not fall back to fresh defaults`() {
        for (version in listOf("-1", "1.5", "oops", "9223372036854775808", "null")) {
            val file = temporary.newFile()
            val original = JSONObject().put("Version", version).put("Value", "must not load").toString()
            file.writeText(original)
            val store = storage()
            assertFalse(store.load(file), version)
            store.save(file)
            assertEquals(original, file.readText())
        }
    }

    @Test
    fun `failed reload revokes save permission and retains the future file`() {
        val file = temporary.newFile()
        file.writeText("""{"Version":"1","Value":"loaded"}""")
        val store = storage()
        assertTrue(store.load(file))
        val future = """{"Version":"2","Value":"future","Other":42}"""
        file.writeText(future)
        assertFalse(store.load(file))
        assertFalse(store.isLoaded)
        store.save(file)
        assertEquals(future, file.readText())
        assertEquals("loaded", fields.storageValue)
    }

    @Test
    fun `missing file on reload cannot be recreated from stale memory`() {
        val file = temporary.newFile()
        file.writeText("""{"Version":"1","Value":"loaded"}""")
        val store = storage()
        assertTrue(store.load(file))
        assertTrue(file.delete())
        repeat(3) {
            assertFalse(store.load(file))
            store.save(file)
            assertFalse(file.exists())
        }
    }

    @Test
    fun `rejected initial storage cannot become fresh defaults on a missing-file retry`() {
        val file = temporary.newFile()
        file.writeText("""{"Version":"99","Value":"future"}""")
        val store = storage()
        assertFalse(store.load(file))
        assertTrue(file.delete())
        repeat(3) {
            assertFalse(store.load(file))
            assertFalse(store.trySave(file))
            assertFalse(file.exists())
        }
        file.writeText("""{"Version":"1","Value":"recovered"}""")
        assertTrue(store.load(file))
        assertTrue(store.trySave(file))
        assertEquals("recovered", fields.storageValue)
    }

    @Test
    fun `malformed JSON and invalid field values block subsequent saves`() {
        for (invalid in listOf("{", """{"Version":"1","Value":{}}""")) {
            val file = temporary.newFile()
            file.writeText("""{"Version":"1","Value":"before"}""")
            val store = storage()
            assertTrue(store.load(file))
            file.writeText(invalid)
            assertFalse(store.load(file))
            assertFalse(store.trySave(file))
            assertEquals(invalid, file.readText())
        }
    }

    @Test
    fun `current storage survives a load save and fresh engine load`() {
        val file = temporary.newFile()
        file.writeText("""{"Version":"1","Value":"nonempty"}""")
        assertTrue(storage().also { assertTrue(it.load(file)); it.save(file) }.isLoaded)
        fields.storageValue = "reset"
        assertTrue(storage().load(file))
        assertEquals("nonempty", fields.storageValue)
    }

    @Test
    fun `supported legacy keys are backed up and loaded`() {
        val file = temporary.newFolder().resolve("storage.json")
        val original = """{"version":"1","value":"legacy"}"""
        file.writeText(original)
        val store = storage()
        assertTrue(store.load(file))
        assertEquals("legacy", fields.storageValue)
        assertEquals(original, file.resolveSibling("storage.json.pre-1.22.bak").readText())
        store.save(file)
        assertEquals("legacy", JSONObject(file.readText()).getString("Value"))
    }

    @Test
    fun `only pending migrations up to the supported version run`() {
        val file = temporary.newFile()
        file.writeText("""{"Version":"1","Value":"before"}""")
        val calls = mutableListOf<Int>()
        val store = storage(2)
        for (version in listOf(1, 2, 3)) {
            store.addMigrationTo(version.toLong(), "version $version") { calls += version }
        }
        assertTrue(store.load(file))
        assertEquals(listOf(2), calls)
        assertEquals(2L, fields.storageVersion)
    }

    @Test
    fun `failed migration cannot publish partially migrated state`() {
        val file = temporary.newFile()
        val original = """{"Version":"1","Value":"before"}"""
        file.writeText(original)
        val store = storage(2)
        store.addMigrationTo(2, "broken") { json ->
            json!!.put("Value", "partial")
            throw IllegalArgumentException("invalid legacy enum")
        }
        assertFalse(store.load(file))
        store.save(file)
        assertEquals(original, file.readText())
        assertEquals("initial", fields.storageValue)
    }

    @Test
    fun `new storage initializes defaults without a file`() {
        val file = temporary.newFolder().resolve("storage.json")
        val store = storage()
        assertTrue(store.load(file))
        store.save(file)
        assertEquals("initial", JSONObject(file.readText()).getString("Value"))
    }
}
