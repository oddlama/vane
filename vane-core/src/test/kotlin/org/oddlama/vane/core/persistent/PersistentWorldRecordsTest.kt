package org.oddlama.vane.core.persistent

import java.io.IOException
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.json.JSONObject

/** The real snapshot reader with a read-only PDC double; no CraftWorld or server is emulated. */
class PersistentWorldRecordsTest {
    private val world = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val good = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val bad = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val prefix = "vane_test:records."

    private fun key(suffix: String) = requireNotNull(NamespacedKey.fromString(prefix + suffix))

    private fun bytes(value: String) =
        JSONObject().put("value", value).toString().toByteArray(Charsets.UTF_8)

    private fun container(records: Map<NamespacedKey, Any>): PersistentDataContainer =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(PersistentDataContainer::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getKeys" -> records.keys
                "get" -> {
                    assertEquals(PersistentDataType.BYTE_ARRAY, args!![1])
                    records[args[0]] as? ByteArray
                }
                else -> error("Snapshot reading must not call ${method.name}")
            }
        } as PersistentDataContainer

    @Test
    fun `complete nonempty snapshot decodes UTF8 and ignores unrelated keys`() {
        val first = bytes("Für Spieler")
        val second = bytes("retained")
        val records =
            linkedMapOf<NamespacedKey, Any>(
                key(good.toString()) to first,
                key(bad.toString()) to second,
            )
        records[NamespacedKey.fromString("another:opaque")!!] = 42
        val result =
            container(records).readStoredJsonRecords(prefix) { _, json -> json.getString("value") }
        assertEquals(mapOf(good to "Für Spieler", bad to "retained"), result)
        assertEquals(3, records.size)
    }

    @Test
    fun `world without records loads empty and permits saving`() {
        val records =
            linkedMapOf<NamespacedKey, Any>(NamespacedKey.fromString("another:opaque")!! to 42)
        val guard = WorldStorageGuard()
        var result: Map<UUID, String>? = null
        guard.load(world) {
            result =
                container(records).readStoredJsonRecords(prefix) { _, json ->
                    json.getString("value")
                }
        }
        assertEquals(emptyMap(), result)
        assertTrue(guard.save(world) {})
        assertEquals(1, records.size)
    }

    private fun rejectedRecord(
        badKey: NamespacedKey = key(bad.toString()),
        value: Any = bytes("bad"),
        decode: (UUID, JSONObject) -> String = { _, json -> json.getString("value") },
    ) {
        val goodBytes = bytes("good")
        val records = linkedMapOf(key(good.toString()) to goodBytes, badKey to value)
        val guard = WorldStorageGuard()
        val failure =
            assertFailsWith<IOException> {
                guard.load(world) { container(records).readStoredJsonRecords(prefix, decode) }
            }
        assertTrue(failure.message!!.contains(badKey.toString()))
        assertFalse(
            guard.save(world) {
                error("No world publication or deletion is permitted after a bad record")
            }
        )
        assertEquals(2, records.size)
        assertTrue(goodBytes.contentEquals(bytes("good")))
        assertEquals(value, records[badKey])
    }

    @Test fun `malformed UUID rejects the entire snapshot`() = rejectedRecord(key("not-a-uuid"))

    @Test
    fun `noncanonical UUID cannot silently alias another key`() = rejectedRecord(key("0-0-0-0-3"))

    @Test fun `wrong PDC type is not silently skipped`() = rejectedRecord(value = 42)

    @Test
    fun `malformed JSON rejects the entire snapshot`() = rejectedRecord(value = "{".toByteArray())

    @Test
    fun `record decoder failure blocks publication even after a valid record`() =
        rejectedRecord { id, json ->
            if (id == bad) throw IllegalArgumentException("unknown record enum")
            json.getString("value")
        }
}
