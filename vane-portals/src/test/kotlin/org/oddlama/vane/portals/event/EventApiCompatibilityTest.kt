package org.oddlama.vane.portals.event

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.portals.portal.PortalBoundary
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class EventApiCompatibilityTest {
    /** Public methods of the Java events before v1.22.0, with their return types. */
    private val javaAccessors: Map<KClass<*>, Map<String, Class<*>>> = mapOf(
        EntityMoveEvent::class to mapOf("getEntity" to Entity::class.java, "getFrom" to Location::class.java, "getTo" to Location::class.java),
        PortalActivateEvent::class to mapOf("getPlayer" to Player::class.java, "getPortal" to Portal::class.java, "getTarget" to Portal::class.java),
        PortalChangeSettingsEvent::class to mapOf("getPlayer" to Player::class.java, "getPortal" to Portal::class.java, "checkOnly" to Boolean::class.java, "isCancelled" to Boolean::class.java),
        PortalConstructEvent::class to mapOf("getPlayer" to Player::class.java, "getBoundary" to PortalBoundary::class.java, "checkOnly" to Boolean::class.java),
        PortalDeactivateEvent::class to mapOf("getPlayer" to Player::class.java, "getPortal" to Portal::class.java),
        PortalDestroyEvent::class to mapOf("getPlayer" to Player::class.java, "getPortal" to Portal::class.java, "checkOnly" to Boolean::class.java, "isCancelled" to Boolean::class.java),
        PortalLinkConsoleEvent::class to mapOf("getPlayer" to Player::class.java, "getConsole" to Block::class.java, "getPortalBlocks" to List::class.java, "getPortal" to Portal::class.java, "checkOnly" to Boolean::class.java, "isCancelled" to Boolean::class.java),
        PortalOpenConsoleEvent::class to mapOf("getPlayer" to Player::class.java, "getConsole" to Block::class.java, "getPortal" to Portal::class.java),
        PortalSelectTargetEvent::class to mapOf("getPlayer" to Player::class.java, "getPortal" to Portal::class.java, "getTarget" to Portal::class.java, "checkOnly" to Boolean::class.java),
        PortalUnlinkConsoleEvent::class to mapOf("getPlayer" to Player::class.java, "getConsole" to Block::class.java, "getPortal" to Portal::class.java, "checkOnly" to Boolean::class.java, "isCancelled" to Boolean::class.java),
    )

    /** Public fields that v1.22.0 to v1.23.0 exposed through @JvmField. */
    private val kotlinFields: Map<KClass<*>, List<String>> = mapOf(
        EntityMoveEvent::class to listOf("entity", "from", "to"),
        PortalActivateEvent::class to listOf("player", "portal"),
        PortalChangeSettingsEvent::class to listOf("player", "portal"),
        PortalConstructEvent::class to listOf("player", "boundary"),
        PortalDeactivateEvent::class to listOf("player", "portal"),
        PortalDestroyEvent::class to listOf("player", "portal"),
        PortalLinkConsoleEvent::class to listOf("player", "console", "portal"),
        PortalOpenConsoleEvent::class to listOf("player", "console", "portal"),
        PortalSelectTargetEvent::class to listOf("player", "portal", "target"),
        PortalUnlinkConsoleEvent::class to listOf("player", "console", "portal"),
    )

    @Test
    fun `java accessors exist with their original signatures`() {
        val missing = mutableListOf<String>()
        for ((cls, methods) in javaAccessors) {
            for ((name, returnType) in methods) {
                val method = try {
                    cls.java.getMethod(name)
                } catch (_: NoSuchMethodException) {
                    missing += "${cls.simpleName}.$name()"
                    continue
                }
                assertEquals(returnType, method.returnType, "${cls.simpleName}.$name()")
                assertTrue(!Modifier.isStatic(method.modifiers), "${cls.simpleName}.$name()")
            }
            val handlerList = cls.java.getMethod("getHandlerList")
            assertTrue(Modifier.isStatic(handlerList.modifiers), "${cls.simpleName}.getHandlerList()")
            assertEquals(HandlerList::class.java, handlerList.returnType)
        }
        if (missing.isNotEmpty()) fail("Missing accessors: $missing")
    }

    @Test
    fun `public fields of the kotlin releases remain`() {
        for ((cls, fields) in kotlinFields) {
            for (name in fields) {
                val field = cls.java.getField(name)
                assertTrue(Modifier.isPublic(field.modifiers), "${cls.simpleName}.$name")
            }
        }
    }
}
