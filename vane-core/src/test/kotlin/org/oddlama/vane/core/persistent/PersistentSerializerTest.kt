package org.oddlama.vane.core.persistent

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentSerializerTest {
    @Suppress("unused")
    private class Holder {
        lateinit var map: MutableMap<String, UUID?>
        lateinit var set: MutableSet<String?>
        lateinit var list: MutableList<Int?>
        lateinit var nested: MutableMap<String, MutableList<String?>?>
    }

    private fun field(name: String) = Holder::class.java.getDeclaredField(name)

    /** Saves [value] into [fieldName] and loads it back, as one restart does. */
    private fun roundTrip(fieldName: String, value: Any): Any? {
        // Through text, as storage.json is written and read.
        val reparsed = when (val json = PersistentSerializer.toJson(field(fieldName), value)) {
            is JSONObject -> JSONObject(json.toString())
            is JSONArray -> JSONArray(json.toString())
            else -> json
        }
        return PersistentSerializer.fromJson(field(fieldName), reparsed)
    }

    @Test
    fun `keeps null map values`() {
        val id = UUID.randomUUID()
        val map = mutableMapOf("a" to id, "b" to null)
        val json = PersistentSerializer.toJson(field("map"), map) as JSONObject
        assertTrue(json.has("b") && json.isNull("b"))
        assertEquals(map, roundTrip("map", map))
    }

    @Test
    fun `keeps null set members`() {
        val set = mutableSetOf("a", null)
        assertEquals(set, roundTrip("set", set))
    }

    @Test
    fun `keeps null list members in place`() {
        val list = mutableListOf(1, null, 3)
        assertEquals(list, roundTrip("list", list))
    }

    @Test
    fun `keeps nulls inside nested collections across repeated saves`() {
        var value: Any = mutableMapOf("x" to mutableListOf("a", null), "y" to null)
        val expected = value
        repeat(3) { value = roundTrip("nested", value)!! }
        assertEquals(expected, value)
    }
}
