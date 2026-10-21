package org.oddlama.vane.portals.portal

import java.io.IOException
import java.util.*
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.EndGateway
import org.bukkit.block.data.FaceAttachable.AttachedFace
import org.bukkit.block.data.type.Switch
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalActivateEvent
import org.oddlama.vane.portals.event.PortalDeactivateEvent
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyleKey
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.LazyLocation

/** Persistent portal model containing ownership, geometry, style, and targeting state. */
class Portal {
    /** Unique portal id. */
    private var id: UUID? = null

    /** UUID of the owning player. */
    private var owner: UUID? = null

    /** Facing/orientation of this portal. */
    private var orientation: Orientation? = null

    /** Spawn location used when exiting this portal. */
    private var spawn: LazyLocation? = null

    /** All blocks currently belonging to this portal structure. */
    private var blocks: MutableList<PortalBlock>? = ArrayList<PortalBlock>()

    /** Display name shown in menus and markers. */
    private var name: String? = "Portal"

    /** Style key used when no local style override is active. */
    private var style: NamespacedKey? = defaultStyleKey()

    /** Optional non-persistent style override. */
    private var styleOverride: Style? = null

    /** Optional custom icon shown for this portal. */
    private var icon: ItemStack? = null

    /** Visibility mode controlling who can target this portal. */
    private var visibility: Visibility? = Visibility.PRIVATE

    /** Whether exit orientation should be preserved/locked on target transfer. */
    private var exitOrientationLocked = false

    /** Preferred fixed target portal id, if any. */
    private var targetId: UUID? = null

    /** Whether the target selection is locked. */
    private var targetLocked = false

    /**
     * Whether this portal has unsaved changes and must be written out on the next save pass.
     *
     * Set by every mutating setter and cleared once the portal has been persisted. This flag is
     * itself transient and is never serialized.
     */
    @JvmField var invalidated: Boolean = true

    /** Constructor used by deserialization. */
    private constructor()

    /** Creates a new portal owned by [owner] with [orientation] and [spawn] location. */
    constructor(owner: UUID?, orientation: Orientation?, spawn: Location) {
        this.id = UUID.randomUUID()
        this.owner = owner
        this.orientation = orientation
        this.spawn = LazyLocation(spawn.clone())
    }

    /**
     * Unique identifier for this portal.
     *
     * @return UUID of the portal or null when not assigned
     */
    fun id() = id

    /**
     * Returns the owner UUID of this portal.
     *
     * @return owner UUID or null
     */
    fun owner() = owner

    /**
     * Returns the portal orientation.
     *
     * @return Orientation or null
     */
    fun orientation() = orientation

    /**
     * Returns the world id of the portal's spawn location.
     *
     * @return UUID of the world
     */
    fun spawnWorld() = spawn!!.worldId

    /**
     * Return a clone of the portal's spawn Location.
     *
     * @return Location clone
     */
    fun spawn() = spawn!!.location().clone()

    /**
     * Return all PortalBlock entries that make up this portal.
     *
     * @return mutable list of PortalBlock instances
     */
    fun blocks() = blocks

    /**
     * Return the display name used for menus and markers.
     *
     * @return portal name or null
     */
    fun name() = name

    /**
     * Set the display name for this portal and mark it as needing persistence.
     *
     * @param name new display name (nullable)
     */
    fun name(name: String?) {
        this.name = name
        this.invalidated = true
    }

    /**
     * Returns the persistent style key when a key-based style is active, or null when a runtime
     * override is used.
     *
     * @return NamespacedKey of the style or null
     */
    fun style() = if (styleOverride == null) style else null

    /**
     * Apply a style to this portal. If the style has a key it will be used as the persistent style
     * key, otherwise the style is stored as a runtime override.
     *
     * @param style the style to apply
     */
    fun style(style: Style) {
        if (style.key() == null) {
            this.styleOverride = style
        } else {
            this.style = style.key()
        }
        this.invalidated = true
    }

    /**
     * Return a clone of the icon ItemStack if present.
     *
     * @return cloned ItemStack or null
     */
    fun icon() = icon?.clone()

    /**
     * Set a custom icon for the portal and mark it for persistence.
     *
     * @param icon ItemStack to use as icon (nullable)
     */
    fun icon(icon: ItemStack?) {
        this.icon = icon
        this.invalidated = true
    }

    /**
     * Returns the visibility mode controlling who can target this portal.
     *
     * @return Visibility enum value
     */
    fun visibility() = visibility

    /**
     * Set the portal visibility mode and mark for persistence.
     *
     * @param visibility new visibility mode
     */
    fun visibility(visibility: Visibility?) {
        this.visibility = visibility
        this.invalidated = true
    }

    /** Return whether exit orientation locking is enabled for this portal. */
    fun exitOrientationLocked() = exitOrientationLocked

    /**
     * Enable or disable exit orientation locking and mark for persistence.
     *
     * @param exitOrientationLocked whether orientation locking should be enabled
     */
    fun exitOrientationLocked(exitOrientationLocked: Boolean) {
        this.exitOrientationLocked = exitOrientationLocked
        this.invalidated = true
    }

    /** Return the fixed target portal id, if set. */
    fun targetId() = targetId

    /**
     * Set a fixed target portal id and mark portal for persistence.
     *
     * @param targetId UUID of the fixed target portal (nullable)
     */
    fun targetId(targetId: UUID?) {
        this.targetId = targetId
        this.invalidated = true
    }

    /** Return whether the portal's target selection is locked. */
    fun targetLocked() = targetLocked

    /**
     * Set the target-locked state and mark for persistence.
     *
     * @param targetLocked whether the target should be locked
     */
    fun targetLocked(targetLocked: Boolean) {
        this.targetLocked = targetLocked
        this.invalidated = true
    }

    /** Return the PortalBlock that matches the given block, or null when none match. */
    fun portalBlockFor(block: Block?) = blocks().orEmpty().firstOrNull { it.block() == block }

    /** Resolve the current target Portal using the provided Portals instance. */
    fun target(portals: Portals) = portals.portalFor(targetId())

    /** Collects blocks that can host controlling levers for this portal. */
    private fun controllingBlocks(): MutableSet<Block> {
        val controllingBlocks = HashSet<Block>()
        for (pb in blocks().orEmpty()) {
            when (pb.type()) {
                PortalBlock.Type.ORIGIN,
                PortalBlock.Type.BOUNDARY1,
                PortalBlock.Type.BOUNDARY2,
                PortalBlock.Type.BOUNDARY3,
                PortalBlock.Type.BOUNDARY4,
                PortalBlock.Type.BOUNDARY5 -> controllingBlocks.add(pb.block()!!)

                PortalBlock.Type.CONSOLE -> {
                    controllingBlocks.add(pb.block()!!)
                    val adj = BlockUtil.adjacentBlocks3D(pb.block()!!)
                    for (b in adj) {
                        if (b != null) controllingBlocks.add(b)
                    }
                }

                else -> {}
            }
        }
        return controllingBlocks
    }

    /** Powers/unpowers all controlling levers attached to this portal. */
    private fun setControllingLevers(activated: Boolean) {
        val controllingBlocks = controllingBlocks()
        val levers = HashSet<Block>()
        for (b in controllingBlocks) {
            for (f in BlockUtil.BLOCK_FACES) {
                val l = b.getRelative(f!!)
                if (l.type != Material.LEVER) {
                    continue
                }

                val lever = l.blockData as Switch
                val attachedFace: BlockFace =
                    when (lever.attachedFace) {
                        AttachedFace.WALL -> lever.facing.oppositeFace
                        AttachedFace.CEILING -> BlockFace.UP
                        AttachedFace.FLOOR -> BlockFace.DOWN
                    }

                // Only when attached to a controlling block
                if (!controllingBlocks.contains(l.getRelative(attachedFace))) {
                    continue
                }

                levers.add(l)
            }
        }

        for (l in levers) {
            val lever = l.blockData as Switch
            lever.isPowered = activated
            l.blockData = lever
            BlockUtil.updateLever(l, lever.facing)
        }
    }

    /**
     * Attempt to activate this portal, dispatching a PortalActivateEvent.
     *
     * @param portals module instance used for activation
     * @param player optional player causing activation
     * @return true when activation succeeded
     */
    fun activate(portals: Portals, player: Player?): Boolean {
        if (portals.isActivated(this)) {
            return false
        }

        val target = target(portals) ?: return false

        // Call event
        val event = PortalActivateEvent(player, this, target)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            return false
        }

        portals.connectPortals(this, target)
        return true
    }

    /**
     * Attempt to deactivate this portal, dispatching a PortalDeactivateEvent.
     *
     * @param portals module instance used for deactivation
     * @param player optional player causing deactivation
     * @return true when deactivation succeeded
     */
    fun deactivate(portals: Portals, player: Player?): Boolean {
        if (!portals.isActivated(this)) {
            return false
        }

        // Call event
        val event = PortalDeactivateEvent(player, this)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            return false
        }

        portals.disconnectPortals(this)
        return true
    }

    /** Apply visual and lever state when connected to the given target portal. */
    fun onConnect(portals: Portals, target: Portal?) {
        // Update blocks
        updateBlocks(portals)

        // Activate all controlling levers
        setControllingLevers(true)

        val soundVolume = portals.configVolumeActivation.toFloat()
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .world
                .playSound(
                    spawn(),
                    Sound.BLOCK_END_PORTAL_SPAWN,
                    SoundCategory.BLOCKS,
                    soundVolume,
                    0.8f,
                )
        }
    }

    /** Apply visual and lever state when disconnected from the given target portal. */
    fun onDisconnect(portals: Portals, target: Portal?) {
        // Update blocks
        updateBlocks(portals)

        // Deactivate all controlling levers
        setControllingLevers(false)

        val soundVolume = portals.configVolumeDeactivation.toFloat()
        if (soundVolume > 0.0f) {
            // Play sound
            spawn()
                .world
                .playSound(
                    spawn(),
                    Sound.BLOCK_END_PORTAL_FRAME_FILL,
                    SoundCategory.BLOCKS,
                    soundVolume,
                    0.5f,
                )
        }
    }

    /** Update all portal block materials according to the current activation and style state. */
    fun updateBlocks(portals: Portals) {
        val curStyle: Style? =
            if (styleOverride == null) {
                portals.style(style)
            } else {
                styleOverride
            }

        val active = portals.isActivated(this)
        for (portalBlock in blocks!!) {
            val type = curStyle!!.material(active, portalBlock.type()!!)
            portalBlock.block()!!.type = type!!
            if (type == Material.END_GATEWAY) {
                // Disable beam
                val endGateway = portalBlock.block()!!.getState(false) as EndGateway
                endGateway.age = 200L
                endGateway.update(true, false)

                // If there's no exit location, then the game will generate a natural gateway when
                // the portal is used.
                // Setting any location will do, since the teleporting is canceled via their events
                // anyway.
                if (spawn!!.location().world.environment == World.Environment.THE_END) {
                    endGateway.exitLocation = spawn!!.location()
                    endGateway.isExactTeleport = true
                }
            }
            if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
                portals.updateConsoleItem(this, portalBlock.block()!!)
            }
        }
    }

    /**
     * Open this portal's console menu for the specified player, dispatching a
     * PortalOpenConsoleEvent.
     *
     * @return true when a menu was opened
     */
    fun openConsole(portals: Portals, player: Player, console: Block?): Boolean {
        // Call event
        val event = PortalOpenConsoleEvent(player, console, this)
        portals.server.pluginManager.callEvent(event)
        if (event.isCancelled && !player.hasPermission(portals.adminPermission)) {
            return false
        }

        val consoleMenu = portals.menus?.consoleMenu
        if (consoleMenu == null) {
            portals.logger.warning("ConsoleMenu is not available")
            return false
        }
        consoleMenu.create(this, player, console).open(player)
        return true
    }

    /** Return a copy of the active style using the provided new key. */
    fun copyStyle(portals: Portals, newKey: NamespacedKey?): Style {
        if (styleOverride == null) {
            val base =
                portals.style(style)
                    ?: portals.style(defaultStyleKey())
                    ?: throw RuntimeException("No base style available to copy")
            return base.copy(newKey)
        }
        return styleOverride!!.copy(newKey)
    }

    /** Returns short debug representation. */
    override fun toString(): String {
        return "Portal{id = $id, name = $name}"
    }

    /** Visibility policy for target selection and discoverability. */
    enum class Visibility {
        PUBLIC,
        GROUP,
        GROUP_INTERNAL,
        PRIVATE;

        /** Returns the previous visibility enum value with wrap-around. */
        fun prev(): Visibility {
            val prev: Int =
                if (ordinal == 0) {
                    entries.size - 1
                } else {
                    ordinal - 1
                }
            return entries[prev]
        }

        /** Returns the next visibility enum value with wrap-around. */
        fun next(): Visibility {
            val next: Int = (ordinal + 1) % entries.size
            return entries[next]
        }

        /** Whether this visibility implies transient/manual target selection semantics. */
        val isTransientTarget: Boolean
            get() = this == GROUP || this == PRIVATE

        /** Whether this visibility mode requires regions integration to function. */
        fun requiresRegions(): Boolean {
            return this == GROUP || this == GROUP_INTERNAL
        }
    }

    /** Sorts target portals by world affinity and planar distance to the given player. */
    class TargetSelectionComparator(player: Player) : Comparator<Portal?> {
        /** Player world used for world-priority sorting. */
        private val world: World = player.location.world

        /** Player position flattened to y=0 for horizontal-distance sorting. */
        private val from: Vector = player.location.toVector().setY(0.0)

        /** Compares two candidate target portals for menu ordering. */
        override fun compare(a: Portal?, b: Portal?): Int {
            if (a == null && b == null) return 0
            if (a == null) return 1
            if (b == null) return -1

            val aSameWorld = world == a.spawn().world
            val bSameWorld = world == b.spawn().world

            if (aSameWorld) {
                if (bSameWorld) {
                    val aDist = from.distanceSquared(a.spawn().toVector().setY(0.0))
                    val bDist = from.distanceSquared(b.spawn().toVector().setY(0.0))
                    return aDist.compareTo(bDist)
                } else {
                    return -1
                }
            } else {
                return if (bSameWorld) {
                    1
                } else {
                    a.name()!!.compareTo(b.name()!!, ignoreCase = true)
                }
            }
        }
    }

    /** JSON serialization helpers for portal persistence. */
    companion object {
        /** Serializes a [Portal] instance into a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val portal = o as Portal
            val json = JSONObject()
            json.put("id", PersistentSerializer.toJson(UUID::class.java, portal.id))
            json.put("owner", PersistentSerializer.toJson(UUID::class.java, portal.owner))
            json.put(
                "orientation",
                PersistentSerializer.toJson(Orientation::class.java, portal.orientation),
            )
            json.put("spawn", PersistentSerializer.toJson(LazyLocation::class.java, portal.spawn))
            try {
                json.put(
                    "blocks",
                    PersistentSerializer.toJson(
                        Portal::class.java.getDeclaredField("blocks"),
                        portal.blocks,
                    ),
                )
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }

            json.put("name", PersistentSerializer.toJson(String::class.java, portal.name))
            json.put("style", PersistentSerializer.toJson(NamespacedKey::class.java, portal.style))
            json.put(
                "styleOverride",
                PersistentSerializer.toJson(Style::class.java, portal.styleOverride),
            )
            json.put("icon", PersistentSerializer.toJson(ItemStack::class.java, portal.icon))
            json.put(
                "visibility",
                PersistentSerializer.toJson(Visibility::class.java, portal.visibility),
            )

            json.put(
                "exitOrientationLocked",
                PersistentSerializer.toJson(
                    Boolean::class.javaPrimitiveType,
                    portal.exitOrientationLocked,
                ),
            )
            json.put("targetId", PersistentSerializer.toJson(UUID::class.java, portal.targetId))
            json.put(
                "targetLocked",
                PersistentSerializer.toJson(Boolean::class.javaPrimitiveType, portal.targetLocked),
            )
            return json
        }

        /** Deserializes a [Portal] instance from a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): Portal {
            val json = o as JSONObject
            val portal = Portal()
            portal.id = PersistentSerializer.fromJson(UUID::class.java, json.get("id"))
            portal.owner = PersistentSerializer.fromJson(UUID::class.java, json.get("owner"))
            portal.orientation =
                PersistentSerializer.fromJson(Orientation::class.java, json.get("orientation"))
            portal.spawn =
                PersistentSerializer.fromJson(LazyLocation::class.java, json.get("spawn"))
            try {
                @Suppress("UNCHECKED_CAST")
                portal.blocks =
                    PersistentSerializer.fromJson(
                        Portal::class.java.getDeclaredField("blocks"),
                        json.get("blocks"),
                    ) as MutableList<PortalBlock>?
            } catch (e: NoSuchFieldException) {
                throw RuntimeException("Invalid field. This is a bug.", e)
            }

            portal.name = PersistentSerializer.fromJson(String::class.java, json.get("name"))
            portal.style =
                PersistentSerializer.fromJson(NamespacedKey::class.java, json.get("style"))
            portal.styleOverride =
                PersistentSerializer.fromJson(Style::class.java, json.get("styleOverride"))
            if (portal.styleOverride != null) {
                try {
                    portal.styleOverride!!.checkValid()
                } catch (_: RuntimeException) {
                    portal.styleOverride = null
                }
            }
            portal.icon = PersistentSerializer.fromJson(ItemStack::class.java, json.get("icon"))
            portal.visibility =
                PersistentSerializer.fromJson(Visibility::class.java, json.get("visibility"))

            portal.exitOrientationLocked =
                PersistentSerializer.fromJson(
                    Boolean::class.javaPrimitiveType,
                    json.optString("exitOrientationLocked", "false"),
                ) ?: false
            portal.targetId = PersistentSerializer.fromJson(UUID::class.java, json.get("targetId"))
            portal.targetLocked =
                PersistentSerializer.fromJson(
                    Boolean::class.javaPrimitiveType,
                    json.get("targetLocked"),
                ) ?: false
            return portal
        }
    }
}
