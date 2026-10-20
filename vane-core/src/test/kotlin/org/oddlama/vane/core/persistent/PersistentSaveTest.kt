package org.oddlama.vane.core.persistent

import org.json.JSONObject
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.module.Module
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentSaveTest {
    @get:Rule val temporary = TemporaryFolder()

    private class Fields {
        @Persistent var storageValue = "before"
    }
    private class BrokenValue
    private class BrokenField {
        @Persistent var storageBroken = BrokenValue()
    }

    private val fields = Fields()
    private val dirty = PersistentStorageDirtyState()
    private val log = Logger.getAnonymousLogger().apply { level = Level.OFF }
    private val original = """{"Version":"1","Value":"before"}"""

    private fun file() = temporary.newFolder().resolve("storage.json").also { it.writeText(original) }
    private fun storage(replace: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }) = PersistentStorage({ 1 }, { "Version" }, { log }, replace).also {
        it.compile(fields) { name -> name }
    }

    @Test
    fun `temporary write failure preserves old bytes and retries`() {
        val file = file()
        val store = storage()
        assertTrue(store.load(file))
        fields.storageValue = "after"
        val temp = File("${file.absolutePath}.tmp")
        assertTrue(temp.mkdir()) // Portable failure injection, including when tests run as root.
        dirty.markDirty()
        assertFalse(dirty.save { store.trySave(file) })
        assertTrue(dirty.isDirty)
        assertEquals(original, file.readText())
        assertTrue(temp.delete())
        assertTrue(dirty.save { store.trySave(file) })
        assertFalse(dirty.isDirty)
        assertEquals("after", JSONObject(file.readText()).getString("Value"))
    }

    @Test
    fun `atomic replacement failure is not acknowledged and retries`() {
        val file = file()
        var fail = true
        val store = storage { source, target ->
            if (fail) throw IOException("injected atomic move failure")
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
        assertTrue(store.load(file))
        fields.storageValue = "after"
        assertFalse(dirty.save { store.trySave(file) })
        assertTrue(dirty.isDirty)
        assertEquals(original, file.readText())
        fail = false
        assertTrue(dirty.save { store.trySave(file) })
        assertFalse(dirty.isDirty)
        assertEquals("after", JSONObject(file.readText()).getString("Value"))
    }

    private fun failedSerialization(error: Exception) {
        val file = file()
        val store = storage()
        assertTrue(store.load(file))
        store.compile(BrokenField()) { name -> name }
        PersistentSerializer.serializers[BrokenValue::class.java] = PersistentSerializer.Function { throw error }
        try {
            assertFalse(dirty.save { store.trySave(file) })
            assertTrue(dirty.isDirty)
            assertEquals(original, file.readText())
            assertFalse(File("${file.absolutePath}.tmp").exists())
            PersistentSerializer.serializers[BrokenValue::class.java] = PersistentSerializer.Function { "recovered" }
            assertTrue(dirty.save { store.trySave(file) })
            assertFalse(dirty.isDirty)
            assertEquals("recovered", JSONObject(file.readText()).getString("Broken"))
        } finally {
            PersistentSerializer.serializers.remove(BrokenValue::class.java)
        }
    }

    @Test fun `serialization IO failure never publishes a partial document`() = failedSerialization(IOException("injected"))
    @Test fun `serialization runtime failure never publishes a partial document`() = failedSerialization(IllegalArgumentException("injected"))

    @Test
    fun `unloaded storage cannot acknowledge or publish a save`() {
        val file = file()
        assertFalse(dirty.save { storage().trySave(file) })
        assertTrue(dirty.isDirty)
        assertEquals(original, file.readText())
    }

    @Test
    fun `mutations made during a successful save stay pending`() {
        dirty.markDirty()
        assertTrue(dirty.save { dirty.markDirty(); true })
        assertTrue(dirty.isDirty)
        assertTrue(dirty.save { true })
        assertFalse(dirty.isDirty)
    }

    @Test
    fun `unexpected exception does not clear pending changes`() {
        assertFailsWith<IllegalStateException> { dirty.save { error("injected") } }
        assertTrue(dirty.isDirty)
    }

    @Test
    fun `public save methods keep their void JVM signatures`() {
        assertEquals(Void.TYPE, PersistentStorageManager::class.java.getMethod("save", File::class.java).returnType)
        assertEquals(Void.TYPE, Module::class.java.getMethod("savePersistentStorage").returnType)
        assertEquals(Boolean::class.javaPrimitiveType, PersistentStorageManager::class.java.getMethod("trySave", File::class.java).returnType)
    }
}
