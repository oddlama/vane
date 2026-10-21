package org.oddlama.vane.core.persistent

import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.json.JSONException

/** Tests the production write barrier with opaque stored records; not a running world/PDC test. */
class WorldStorageGuardTest {
    private val world = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val guard = WorldStorageGuard()

    @Test
    fun `unseen world never publishes or deletes records`() {
        val stored = mutableMapOf("valid" to "old bytes", "unknown" to "opaque bytes")
        val before = stored.toMap()
        assertFalse(
            guard.save(world) {
                stored.clear()
                stored["valid"] = "replacement"
            }
        )
        assertEquals(before, stored)
    }

    @Test
    fun `save is blocked while a load is in progress`() {
        var writes = 0
        guard.load(world) {
            assertFalse(guard.save(world) { writes++ })
            assertFalse(guard.canSave(world))
        }
        assertTrue(guard.save(world) { writes++ })
        assertEquals(1, writes)
    }

    @Test
    fun `partial load errors retain all stored records including the failed one`() {
        for (failure in
            listOf(
                IOException("read"),
                JSONException("bad JSON"),
                IllegalArgumentException("bad enum"),
            )) {
            val stored = linkedMapOf("good" to "old good bytes", "failed" to "unknown raw bytes")
            val before = stored.toMap()
            val index = mutableMapOf<String, String>()
            assertFailsWith<Exception> {
                guard.load(world) {
                    index["good"] = stored.getValue("good")
                    throw failure
                }
            }
            assertFalse(
                guard.save(world) {
                    stored["good"] = "changed good bytes"
                    stored.keys.retainAll(index.keys)
                }
            )
            assertEquals(before, stored)
        }
    }

    @Test
    fun `failed reload revokes a previous successful load`() {
        guard.load(world) {}
        assertTrue(guard.canSave(world))
        assertFailsWith<IOException> { guard.load(world) { throw IOException("reload failed") } }
        assertFalse(guard.canSave(world))
        assertFalse(guard.save(world) { error("must not run") })
    }

    @Test
    fun `successful load permits intentional deletion`() {
        val stored = mutableMapOf("keep" to "one", "delete" to "two")
        val index = mutableSetOf<String>()
        guard.load(world) { index.addAll(stored.keys) }
        index.remove("delete")
        assertTrue(guard.save(world) { stored.keys.retainAll(index) })
        assertEquals(mapOf("keep" to "one"), stored)
    }

    @Test
    fun `worlds have independent save permissions`() {
        guard.load(world) {}
        assertFailsWith<IOException> { guard.load(other) { throw IOException("failed world") } }
        assertTrue(guard.save(world) {})
        assertFalse(guard.save(other) { error("must not run") })
    }

    @Test
    fun `unload requires another successful load before saving`() {
        guard.load(world) {}
        guard.forget(world)
        assertFalse(guard.save(world) { error("must not run") })
        guard.load(world) {}
        assertTrue(guard.save(world) {})
    }

    @Test
    fun `disable revokes all previous successful loads`() {
        guard.load(world) {}
        guard.load(other) {}
        guard.clear()
        assertFalse(guard.canSave(world))
        assertFalse(guard.canSave(other))
    }

    @Test
    fun `unload during loading cannot be undone by late completion`() {
        guard.load(world) { guard.forget(world) }
        assertFalse(guard.canSave(world))
    }

    @Test
    fun `disable during loading cannot be undone by late completion`() {
        guard.load(world) { guard.clear() }
        assertFalse(guard.canSave(world))
    }

    @Test
    fun `recursive load fails without granting save permission`() {
        assertFailsWith<IllegalStateException> { guard.load(world) { guard.load(world) {} } }
        assertFalse(guard.canSave(world))
    }
}
