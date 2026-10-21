package org.oddlama.vane.portals.portal

import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer

class PortalBlockTypeTest {
    /** Same shape as the material maps of [Style], keyed by portal block type. */
    @Suppress("unused")
    private class Holder {
        lateinit var materials: MutableMap<PortalBlock.Type, String>
    }

    private val materialsField = Holder::class.java.getDeclaredField("materials")

    @BeforeTest
    fun registerSerializers() {
        // Mirrors the registration in the Portals companion, which needs a running plugin.
        PersistentSerializer.serializers[PortalBlock.Type::class.java] =
            PersistentSerializer.Function { x: Any? ->
                (x as PortalBlock.Type).name
            }
        PersistentSerializer.deserializers[PortalBlock.Type::class.java] =
            PersistentSerializer.Function { x: Any? ->
                PortalBlock.Type.fromSerialized((x as String?)!!)
            }
        PersistentSerializer.serializers[PortalBlock::class.java] =
            PersistentSerializer.Function { obj: Any? ->
                PortalBlock.serialize(obj!!)
            }
        PersistentSerializer.deserializers[PortalBlock::class.java] =
            PersistentSerializer.Function { obj: Any? ->
                PortalBlock.deserialize(obj!!)
            }
    }

    @Test
    fun `decodes current and pre-1_22 names`() {
        for (type in PortalBlock.Type.entries) {
            assertEquals(type, PortalBlock.Type.fromSerialized(type.name))
        }
        for (i in 1..5) {
            assertEquals(
                PortalBlock.Type.valueOf("BOUNDARY$i"),
                PortalBlock.Type.fromSerialized("BOUNDARY_$i"),
            )
        }
        // Style configs from before v1.22.0 spell them in lower case.
        assertEquals(PortalBlock.Type.BOUNDARY3, PortalBlock.Type.fromSerialized("boundary_3"))
        assertFailsWith<IllegalArgumentException> { PortalBlock.Type.fromSerialized("BOUNDARY_6") }
    }

    @Test
    fun `loads legacy portal blocks across repeated saves`() {
        val worldId = UUID.randomUUID()
        var json: Any =
            JSONObject()
                .put(
                    "block",
                    JSONObject()
                        .put("worldId", worldId.toString())
                        .put("x", "1")
                        .put("y", "64")
                        .put("z", "-3"),
                )
                .put("type", "BOUNDARY_2")

        // Each iteration is one server restart: load what the previous one saved, then save it.
        repeat(3) {
            val block = PersistentSerializer.fromJson(PortalBlock::class.java, json)!!
            assertEquals(PortalBlock.Type.BOUNDARY2, block.type())
            json = PersistentSerializer.toJson(PortalBlock::class.java, block)!!
            assertEquals("BOUNDARY2", (json as JSONObject).getString("type"))
        }
    }

    @Test
    fun `loads legacy style maps across repeated saves`() {
        var json: Any =
            JSONObject().apply {
                put("ORIGIN", "obsidian")
                put("CONSOLE", "enchanting_table")
                for (i in 1..5) put("BOUNDARY_$i", "boundary$i")
                put("PORTAL", "air")
            }

        repeat(3) {
            @Suppress("UNCHECKED_CAST")
            val materials =
                PersistentSerializer.fromJson(materialsField, json) as Map<PortalBlock.Type, String>
            assertEquals(PortalBlock.Type.entries.toSet(), materials.keys)
            for (i in 1..5) {
                assertEquals("boundary$i", materials[PortalBlock.Type.valueOf("BOUNDARY$i")])
            }
            json = PersistentSerializer.toJson(materialsField, materials)!!
        }
        assertEquals(
            PortalBlock.Type.entries.map { it.name }.toSet(),
            (json as JSONObject).keySet(),
        )
    }
}
