package org.oddlama.vane.portals

import com.google.common.collect.Sets
import net.kyori.adventure.text.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.world.*
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.*
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.core.persistent.WorldStorageGuard
import org.oddlama.vane.core.persistent.readStoredJsonRecords
import org.oddlama.vane.core.persistent.storedUuidsByPrefix
import org.oddlama.vane.portals.entity.FloatingItem
import org.oddlama.vane.portals.menu.PortalMenuGroup
import org.oddlama.vane.portals.menu.PortalMenuTag
import org.oddlama.vane.portals.portal.*
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyle
import org.oddlama.vane.portals.portal.Style.Companion.defaultStyleKey
import org.oddlama.vane.util.*
import java.io.IOException
import java.util.*

@VaneModule(name = "portals", bstats = 8642, configVersion = 3, langVersion = 6, storageVersion = 2)
/** Main module managing portal lifecycle, storage, indexing, UI, and integrations. */
class Portals : Module<Portals?>() {
    /** Set of materials disallowed for decorative portal usage. */
    @ConfigMaterialSet(
        def = [Material.PISTON, Material.STICKY_PISTON],
        desc = "Materials which may not be used to decorate portals."
    )
    var configBlacklistedMaterials: MutableSet<Material?>? = null

    // TODO better and more default styles
    @ConfigMaterialMapMapMap(
        def = [ConfigMaterialMapMapMapEntry(
            key = "vane_portals:portal_style_default",
            value = [ConfigMaterialMapMapEntry(
                key = "Active",
                value = [ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY)]
            ), ConfigMaterialMapMapEntry(
                key = "Inactive",
                value = [ConfigMaterialMapEntry(key = "Boundary1", value = Material.OBSIDIAN), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.OBSIDIAN
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.AIR)]
            )]
        ), ConfigMaterialMapMapMapEntry(
            key = "vane_portals:portal_style_aqua",
            value = [ConfigMaterialMapMapEntry(
                key = "Active",
                value = [ConfigMaterialMapEntry(
                    key = "Boundary1",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.WARPED_PLANKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.SEA_LANTERN
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.WARPED_WART_BLOCK
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.LIGHT_BLUE_STAINED_GLASS
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.END_GATEWAY)]
            ), ConfigMaterialMapMapEntry(
                key = "Inactive",
                value = [ConfigMaterialMapEntry(
                    key = "Boundary1",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(
                    key = "Boundary2",
                    value = Material.WARPED_PLANKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary3",
                    value = Material.PRISMARINE_BRICKS
                ), ConfigMaterialMapEntry(
                    key = "Boundary4",
                    value = Material.WARPED_WART_BLOCK
                ), ConfigMaterialMapEntry(
                    key = "Boundary5",
                    value = Material.LIGHT_BLUE_STAINED_GLASS
                ), ConfigMaterialMapEntry(key = "Console", value = Material.ENCHANTING_TABLE), ConfigMaterialMapEntry(
                    key = "Origin",
                    value = Material.DARK_PRISMARINE
                ), ConfigMaterialMapEntry(key = "Portal", value = Material.AIR)]
            )]
        )],
        desc = "Portal style definitions. Must provide a material for each portal block type and activation state. The default style may be overridden."
    )
            /** Raw style config map loaded from module configuration. */
    var configStyles: MutableMap<String?, MutableMap<String?, MutableMap<String?, Material?>?>?>? = null

    @ConfigLong(
        def = 10000,
        min = 1000,
        max = 110000,
        desc = "Delay in milliseconds after which two connected portals will automatically be disabled. Purple end-gateway beams do not show up until the maximum value of 110 seconds."
    )
            /** Delay before connected portals auto-deactivate. */
    var configDeactivationDelay: Long = 0

    @ConfigExtendedMaterial(
        def = "vane:decoration_end_portal_orb",
        desc = "The default portal icon. Also accepts heads from the head library."
    )
            /** Default icon used when portals do not define a custom icon. */
    var configDefaultIcon: ExtendedMaterial? = null

    @ConfigDouble(
        def = 0.9,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal activation sound effect. 0 to disable."
    )
            /** Activation sound volume. */
    var configVolumeActivation: Double = 0.0

    @ConfigDouble(
        def = 1.0,
        min = 0.0,
        max = 1.0,
        desc = "Volume for the portal deactivation sound effect. 0 to disable."
    )
            /** Deactivation sound volume. */
    var configVolumeDeactivation: Double = 0.0

    /** Console display format for active portals. */
    @LangMessage
    var langConsoleDisplayActive: TranslatedMessage? = null

    /** Console display format for inactive portals. */
    @LangMessage
    var langConsoleDisplayInactive: TranslatedMessage? = null

    /** Placeholder text used when no target portal is selected. */
    @LangMessage
    var langConsoleNoTarget: TranslatedMessage? = null

    /** Message shown when unlink operations are denied. */
    @LangMessage
    var langUnlinkRestricted: TranslatedMessage? = null

    /** Message shown when destroy operations are denied. */
    @LangMessage
    var langDestroyRestricted: TranslatedMessage? = null

    /** Message shown when settings changes are denied. */
    @LangMessage
    var langSettingsRestricted: TranslatedMessage? = null

    /** Message shown when target selection is denied. */
    @LangMessage
    var langSelectTargetRestricted: TranslatedMessage? = null

    // This permission allows players (usually admins) to always modify settings
    // on any portal, regardless of whether other restrictions would block access.
    /** Override permission that bypasses regular ownership restrictions. */
    val adminPermission: Permission

    // Primary storage for all portals (portalId → portal)
    @Persistent
    /** Persisted portal map loaded/saved to world data. */
    private val storagePortals: MutableMap<UUID?, Portal> = HashMap<UUID?, Portal>()

    /** Runtime portal map containing all currently indexed portals. */
    private val portals: MutableMap<UUID?, Portal> = HashMap<UUID?, Portal>()

    private val worldStorage = WorldStorageGuard()

    // Index for all portal blocks (worldId → chunk key → block key → portal block)
    /** Runtime index mapping world/chunk/block to portal-block metadata. */
    private val portalBlocksInChunkInWorld: MutableMap<UUID?, MutableMap<Long?, MutableMap<Long?, PortalBlockLookup?>>> =
        HashMap<UUID?, MutableMap<Long?, MutableMap<Long?, PortalBlockLookup?>>>()

    // All loaded styles
    /** Loaded style definitions keyed by namespaced key. */
    var styles: MutableMap<NamespacedKey?, Style> = HashMap<NamespacedKey?, Style>()

    // Cache possible area materials. This is fine as only predefined styles can
    // change this.
    /** Cache of materials considered portal-area blocks for fast checks. */
    var portalAreaMaterials: MutableSet<Material?> = HashSet()

    // Track console items
    /** Floating item entities currently displayed on portal consoles. */
    private val consoleFloatingItems: MutableMap<Block, FloatingItem?> = HashMap<Block, FloatingItem?>()

    // Connected portals (always stores both directions!)
    /** Bidirectional map of currently connected portals. */
    private val connectedPortals: MutableMap<UUID?, UUID?> = HashMap()

    // Unloading ticket counter per chunk
    /** Reference-counted chunk ticket counters for loaded portal chunks. */
    private val chunkTicketCount: MutableMap<Long?, Int> = HashMap<Long?, Int>()

    // Disable tasks for portals
    /** Scheduled delayed deactivation tasks by portal id. */
    private val disableTasks: MutableMap<UUID?, BukkitTask?> = HashMap()

    /** Aggregated menu components for portal UIs. */
    var menus: PortalMenuGroup?

    /** Portal construction/listener component. */
    var constructor: PortalConstructor

    /** Dynmap integration component. */
    var dynmapLayer: PortalDynmapLayer

    /** BlueMap integration component. */
    var blueMapLayer: PortalBlueMapLayer

    /** Registers custom entities used by this module. */

    private fun registerEntities() {
        core!!.unfreezeRegistries()
        Nms.registerEntity(
            NamespacedKey.minecraft("item"),
            namespace(),
            "floating_item",
            EntityType.Builder.of({ entitytypes, world ->
                FloatingItem(entitytypes, world)
            }, MobCategory.MISC).noSave().sized(0.0f, 0.0f)
        )
    }

    /** Rebuilds style maps and derived caches after configuration changes. */
    override fun onConfigChange() {
        styles.clear()

        configStyles!!.forEach { (styleKey: String?, v1: MutableMap<String?, MutableMap<String?, Material?>?>?) ->
            // Char-delimiter split; the regex overload compiled a fresh Pattern per style.
            val split = styleKey!!.split(':').dropLastWhile { it.isEmpty() }
            if (split.size != 2) {
                throw RuntimeException("Invalid style key: '$styleKey' is not a valid namespaced key")
            }

            val style = Style(StorageUtil.namespacedKey(split[0], split[1]))
            v1!!.forEach { (isActive: String?, v2: MutableMap<String?, Material?>?) ->
                // Configs from before v1.22.0 spell these `active` and `boundary_1`.
                val active: Boolean = when {
                    LegacyKeys.matches(isActive!!, "Active") -> true
                    LegacyKeys.matches(isActive, "Inactive") -> false
                    else -> throw RuntimeException("Invalid active state, must be either 'Active' or 'Inactive'")
                }
                v2!!.forEach { (portalBlockType: String?, material: Material?) ->
                    val type = PortalBlock.Type.fromSerialized(portalBlockType!!)
                    style.setMaterial(active, type, material!!)
                }
            }

            // Check validity and add to map.
            style.checkValid()
            styles[style.key()] = style
        }

        if (!styles.containsKey(defaultStyleKey())) {
            // Add default style if it wasn't overridden
            val defaultStyle = defaultStyle()
            styles[defaultStyle.key()] = defaultStyle
        }

        portalAreaMaterials.clear()
        // Acquire material set from styles. Will be used to speed up event checking.
        for (style in styles.values) {
            portalAreaMaterials.add(style.material(true, PortalBlock.Type.PORTAL))
        }
    }

    // Lightweight callbacks to the regions module, if it is installed.
    // Lifting the callback storage into the portals module saves us
    // from having to ship regions api with this module.
    /** Optional callback to test whether two portals share a region group. */
    private var isInSameRegionGroupCallback: Function2<Portal?, Portal?, Boolean?>? = null

    /**
     * Set a callback used to determine whether two portals are considered in the same region group.
     * @param callback callback returning true when the two portals are in the same group
     */
    fun setIsInSameRegionGroupCallback(callback: Function2<Portal?, Portal?, Boolean?>?) {
        isInSameRegionGroupCallback = callback
    }

    /** Optional callback checking whether a player may use a portal's region group. */
    private var playerCanUsePortalsInRegionGroupOfCallback: Function2<Player?, Portal?, Boolean?>? = null

    /**
     * Set a callback used to determine whether a player may use portals in the region group of a portal.
     * @param callback callback returning true when the player is allowed to use portals in the group
     */
    fun setPlayerCanUsePortalsInRegionGroupOfCallback(
        callback: Function2<Player?, Portal?, Boolean?>?
    ) {
        playerCanUsePortalsInRegionGroupOfCallback = callback
    }

    /**
     * Check whether two portals are considered to be in the same region group.
     * @return true when the two portals are considered in the same group
     */
    fun isInSameRegionGroup(a: Portal?, b: Portal?): Boolean {
        return isInSameRegionGroupCallback == null || isInSameRegionGroupCallback!!.apply(a, b)!!
    }

    /**
     * Return whether the specified player may use portals in the region group of the provided portal.
     * @return true when the player is allowed to use portals in that group
     */
    fun playerCanUsePortalsInRegionGroupOf(player: Player?, portal: Portal?): Boolean {
        return playerCanUsePortalsInRegionGroupOfCallback == null || playerCanUsePortalsInRegionGroupOfCallback!!.apply(
            player,
            portal
        )!!
    }

    /** True when regions integration callbacks are available. */
    val isRegionsInstalled: Boolean
        get() = isInSameRegionGroupCallback != null

    /**
     * Resolve a Style by its namespaced key from configuration or defaults.
     * @return the resolved Style or null if not found
     */

    fun style(key: NamespacedKey?): Style? {
        return styles[key] ?: run {
            logger.warning("Encountered invalid style $key, falling back to default style.")
            styles[defaultStyleKey()]
        }
    }

    /** Removes the given portal and all associated runtime state, blocks, and links. */

    fun removePortal(portal: Portal) {
        // Deactivate portal if needed
        val connected = connectedPortal(portal)
        if (connected != null) {
            disconnectPortals(portal, connected)
        }

        // Remove portal from storage
        if (portals.remove(portal.id()) == null) {
            // Was already removed
            return
        }

        // Remove portal blocks
        portal.blocks().orEmpty().forEach { portalBlock -> removePortalBlock(portalBlock) }

        // Replace references to the portal everywhere
        // and update all changed portal consoles.
        for (other in portals.values) {
            if (other.targetId() == portal.id()) {
                other.targetId(null)
                other
                    .blocks()
                    .orEmpty()
                    .filter { pb -> pb.type() == PortalBlock.Type.CONSOLE }
                    .filter { pb -> consoleFloatingItems.containsKey(pb.block()) }
                    .forEach { pb -> updateConsoleItem(other, pb.block()!!) }
            }
        }

        // Force update storage now, as a precaution.
        updatePersistentData()

        // Close and taint all related open menus
        core!!.menuManager?.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is PortalMenuTag &&
                (menu.tag() as PortalMenuTag).portalId() == portal.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }

        // Remove map marker
        removeMarker(portal.id())

        // Play sound
        portal
            .spawn()
            .world
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 1.0f)
    }

    /** Adds a newly created portal into runtime indices and plays feedback sound. */

    fun addNewPortal(portal: Portal) {
        portal.invalidated = true

        // Index the new portal
        indexPortal(portal)

        // Play sound
        portal
            .spawn()
            .world
            .playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 2.0f)
    }

    /** Ensure a portal is indexed for fast lookup (by id, blocks, chunks). */

    fun indexPortal(portal: Portal) {
        portals[portal.id()] = portal
        portal.blocks().orEmpty().forEach { b -> indexPortalBlock(portal, b) }

        // Create map marker
        updateMarker(portal)
    }

    /** Returns all loaded portals whose spawn worlds are currently available. */

    fun allAvailablePortals(): MutableCollection<Portal?> {
        return portals.values.filter { p -> p.spawn().isWorldLoaded }.toMutableList()
    }

    /** Restores and removes a single portal block from runtime acceleration structures. */

    fun removePortalBlock(portalBlock: PortalBlock) {
        // Restore original block
        when (portalBlock.type()) {
            PortalBlock.Type.ORIGIN -> portalBlock.block()!!.type = constructor.configMaterialOrigin!!
            PortalBlock.Type.CONSOLE -> portalBlock.block()!!.type = constructor.configMaterialConsole!!
            PortalBlock.Type.BOUNDARY1 -> portalBlock.block()!!.type = constructor.configMaterialBoundary1!!
            PortalBlock.Type.BOUNDARY2 -> portalBlock.block()!!.type = constructor.configMaterialBoundary2!!
            PortalBlock.Type.BOUNDARY3 -> portalBlock.block()!!.type = constructor.configMaterialBoundary3!!
            PortalBlock.Type.BOUNDARY4 -> portalBlock.block()!!.type = constructor.configMaterialBoundary4!!
            PortalBlock.Type.BOUNDARY5 -> portalBlock.block()!!.type = constructor.configMaterialBoundary5!!
            PortalBlock.Type.PORTAL -> portalBlock.block()!!.type = constructor.configMaterialPortalArea!!
            else -> {}
        }

        // Remove console item if a block is a console
        if (portalBlock.type() == PortalBlock.Type.CONSOLE) {
            removeConsoleItem(portalBlock.block())
        }

        // Remove from acceleration structure
        val block = portalBlock.block()
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block!!.world.uid] ?: return

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return

        blockToPortalBlock.remove(blockKey(block))

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()!!
                .world
                .spawnParticle(
                    Particle.ENCHANT,
                    portalBlock.block()!!.location.add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                )
        }
    }

    /** Removes the given portalBlock from its portal and global block indices. */

    fun removePortalBlock(portal: Portal, portalBlock: PortalBlock) {
        // Remove from portal
        portal.blocks()!!.remove(portalBlock)

        // Remove from acceleration structure
        removePortalBlock(portalBlock)
    }

    /** Adds the given portalBlock to the portal, indexes it, and spawns placement effects. */

    fun addNewPortalBlock(portal: Portal, portalBlock: PortalBlock?) {
        // Add to portal
        portal.blocks()!!.add(portalBlock!!)
        portal.invalidated = true

        indexPortalBlock(portal, portalBlock)

        // Spawn effect if not portal area
        if (portalBlock.type() != PortalBlock.Type.PORTAL) {
            portalBlock
                .block()!!
                .world
                .spawnParticle(
                    Particle.PORTAL,
                    portalBlock.block()!!.location.add(0.5, 0.5, 0.5),
                    50,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                )
        }
    }

    /** Inserts the portalBlock into the world/chunk/block lookup index for the portal. */

    fun indexPortalBlock(portal: Portal, portalBlock: PortalBlock) {
        // Add to acceleration structure
        val block = portalBlock.block()
        val worldId = block!!.world.uid
        val portalBlocksInChunk =
            portalBlocksInChunkInWorld.computeIfAbsent(worldId) { k: UUID? -> HashMap<Long?, MutableMap<Long?, PortalBlockLookup?>>() }

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock =
            portalBlocksInChunk.computeIfAbsent(chunkKey) { k: Long? -> HashMap() }

        blockToPortalBlock[blockKey(block)] = portalBlock.lookup(portal.id())
    }

    /**
     * Find a PortalBlockLookup for the given Block if it belongs to a known portal.
     * @return PortalBlockLookup or null if the block is not part of a portal
     */

    fun portalBlockFor(block: Block): PortalBlockLookup? {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block.world.uid] ?: return null

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return null

        return blockToPortalBlock[blockKey(block)]
    }

    /**
     * Lookup a portal by UUID.
     * @return the Portal or null when not found
     */

    fun portalFor(uuid: UUID?): Portal? {
        val portal = portals[uuid]
        if (portal == null || !portal.spawn().isWorldLoaded) {
            return null
        }
        return portal
    }

    /** Resolves owning portal for a [PortalBlockLookup]. */

    fun portalFor(block: PortalBlockLookup): Portal {
        return portalFor(block.portalId())!!
    }

    /**
     * Return the Portal owning the given block if any.
     * @return the portal or null
     */

    fun portalFor(block: Block): Portal? {
        val portalBlock = portalBlockFor(block) ?: return null

        return portalFor(portalBlock)
    }

    /** Returns true when the block is indexed as part of any portal structure. */

    fun isPortalBlock(block: Block): Boolean {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[block.world.uid] ?: return false

        val chunkKey = block.chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return false

        return blockToPortalBlock.containsKey(blockKey(block))
    }

    /**
     * Return the portal that controls the given block (e.g., console block) or null.
     */

    fun controlledPortal(block: Block): Portal? {
        val rootPortal = portalFor(block)
        if (rootPortal != null) {
            return rootPortal
        }

        // Find adjacent console blocks in full 3x3x3 cube, which will make this block a
        // controlling block
        for (adj in BlockUtil.adjacentBlocks3D(block)) {
            val portalBlock = portalBlockFor(adj!!)
            if (portalBlock != null && portalBlock.type() == PortalBlock.Type.CONSOLE) {
                return portalFor(portalBlock)
            }
        }

        return null
    }

    /** Return the set of chunks that must be kept loaded for the portal. */

    fun chunksFor(portal: Portal?): MutableSet<Chunk> {
        if (portal == null) {
            return HashSet<Chunk>()
        }

        val set = HashSet<Chunk>()
        for (pb in portal.blocks()!!) {
            set.add(pb.block()!!.chunk)
        }
        return set
    }

    /** Loads and tickets all chunks used by the portal. */

    fun loadPortalChunks(portal: Portal?) {
        // Load chunks and adds a ticket, so they get loaded and are kept loaded
        for (chunk in chunksFor(portal)) {
            val chunkKey = chunk.chunkKey
            val ticketCounter = chunkTicketCount[chunkKey]
            if (ticketCounter == null) {
                chunk.addPluginChunkTicket(this)
                chunkTicketCount[chunkKey] = 1
            } else {
                chunkTicketCount[chunkKey] = ticketCounter + 1
            }
        }
    }

    /** Decrements/removes chunk tickets for all chunks used by the portal. */

    fun allowUnloadPortalChunks(portal: Portal?) {
        // Removes the ticket so chunks can be unloaded again
        for (chunk in chunksFor(portal)) {
            val chunkKey = chunk.chunkKey
            val ticketCounter: Int = chunkTicketCount[chunkKey]!!

            if (ticketCounter > 1) {
                chunkTicketCount[chunkKey] = ticketCounter - 1
            } else if (ticketCounter == 1) {
                chunk.removePluginChunkTicket(this)
                chunkTicketCount.remove(chunkKey)
            }
        }
    }

    /** Connect two portals (src and dst), activate both portals, and schedule auto-disable. */

    fun connectPortals(src: Portal, dst: Portal) {
        // Load chunks
        loadPortalChunks(src)
        loadPortalChunks(dst)

        // Add to map
        connectedPortals[src.id()] = dst.id()
        connectedPortals[dst.id()] = src.id()

        // Activate both
        src.onConnect(this, dst)
        dst.onConnect(this, src)

        // Schedule automatic disable
        startDisableTask(src, dst)
    }

    /** Disconnects portal pair and handles cleanup of transient target state. */

    fun disconnectPortals(src: Portal?, dst: Portal? = portalFor(connectedPortals[src!!.id()])) {
        if (src == null || dst == null) {
            return
        }

        // Allow unloading chunks again
        allowUnloadPortalChunks(src)
        allowUnloadPortalChunks(dst)

        // Remove from a map
        connectedPortals.remove(src.id())
        connectedPortals.remove(dst.id())

        // Deactivate both
        src.onDisconnect(this, dst)
        dst.onDisconnect(this, src)

        // Reset target id's if the target portal was transient and
        // the target isn't locked.
        if (dst.visibility()!!.isTransientTarget && !src.targetLocked()) {
            src.targetId(null)
            src.updateBlocks(this)
        }

        // Remove an automatic disable task if existing
        stopDisableTask(src, dst)
    }

    /** Starts or replaces delayed auto-disable task for connected portal pair. */
    private fun startDisableTask(portal: Portal, target: Portal) {
        stopDisableTask(portal, target)
        val task = scheduleTask(
            PortalDisableRunnable(portal, target),
            msToTicks(configDeactivationDelay)
        )
        disableTasks[portal.id()] = task
        disableTasks[target.id()] = task
    }

    /** Cancels pending auto-disable task(s) associated with a portal pair. */
    private fun stopDisableTask(portal: Portal, target: Portal) {
        val task1 = disableTasks.remove(portal.id())
        val task2 = disableTasks.remove(target.id())
        task1?.cancel()
        if (task2 != null && task2 !== task1) {
            task2.cancel()
        }
    }

    /** Module shutdown hook: disconnects portals, clears visuals/tickets, and persists state. */
    override fun onModuleDisable() {
        try {
            // Disable all portals now
            for (id in ArrayList(connectedPortals.keys)) {
                disconnectPortals(portalFor(id))
            }

            // Remove all console items, and all chunk tickets
            chunkTicketCount.clear()
            for (world in server.worlds) {
                for (chunk in world.loadedChunks) {
                    // Remove console item
                    forEachConsoleBlockInChunk(
                        chunk
                    ) { block: Block?, console: PortalBlockLookup? -> removeConsoleItem(block) }
                    // Allow chunk unloading
                    chunk.removePluginChunkTicket(this)
                }
            }

            // Save data
            updatePersistentData()
            super.onModuleDisable()
        } finally {
            worldStorage.clear()
        }
    }

    /** Returns true when the portal currently has an active connection. */

    fun isActivated(portal: Portal): Boolean {
        return connectedPortals.containsKey(portal.id())
    }

    /** Returns the currently connected counterpart of the portal, if connected. */

    fun connectedPortal(portal: Portal): Portal? {
        val connectedId = connectedPortals[portal.id()] ?: return null
        return portalFor(connectedId)
    }

    /** Returns icon for the portal, falling back to the configured default icon. */

    fun iconFor(portal: Portal): ItemStack? {
        val item = portal.icon()
        return item ?: configDefaultIcon!!.item()
    }

    /** Builds the hovering console item for the portal in active/inactive state. */
    private fun makeConsoleItem(portal: Portal, active: Boolean): ItemStack {
        val target = if (active) {
            connectedPortal(portal)
        } else {
            portal.target(this)
        }

        // Try to use target portal's block
        var item: ItemStack? = null
        if (target != null) {
            item = target.icon()
        }

        // Fallback item
        if (item == null) {
            item = configDefaultIcon!!.item()
        }

        val targetName = if (target == null) langConsoleNoTarget!!.str() else target.name()
        val displayName: Component = if (active) {
            langConsoleDisplayActive!!.format("§5$targetName")
        } else {
            langConsoleDisplayInactive!!.format("§7$targetName")
        }

        return ItemUtil.nameItem(item!!, displayName)
    }

    /** Refresh marker and dependent console items when a portal's icon/name data changes. */

    fun updatePortalIcon(portal: Portal) {
        // Update map marker, as name could have changed
        updateMarker(portal)

        for (activeConsole in consoleFloatingItems.keys) {
            val portalBlock = portalBlockFor(activeConsole)
            val other = portalFor(portalBlock!!)
            if (other.targetId() == portal.id()) {
                updateConsoleItem(other, activeConsole)
            }
        }
    }

    /** Re-validate target references after a portal's visibility changes and update markers. */

    fun updatePortalVisibility(portal: Portal) {
        // Replace references to the portal everywhere if visibility
        // has changed.
        when (portal.visibility()) {
            Portal.Visibility.PRIVATE, Portal.Visibility.GROUP ->                 // Not visible from outside, these are transient.
                for (other in portals.values) {
                    if (other.targetId() == portal.id()) {
                        other.targetId(null)
                    }
                }

            Portal.Visibility.GROUP_INTERNAL ->                 // Remove from portals outside the group
                for (other in portals.values) {
                    if (other.targetId() == portal.id() && !isInSameRegionGroup(other, portal)) {
                        other.targetId(null)
                    }
                }

            else -> {}
        }

        // Update map marker
        updateMarker(portal)
    }

    /** Create or update the floating console item above the given block for a portal. */

    fun updateConsoleItem(portal: Portal, block: Block) {
        var consoleItem = consoleFloatingItems[block]
        val isNew: Boolean
        if (consoleItem == null) {
            consoleItem = FloatingItem(
                block.world,
                block.x + 0.5,
                block.y + 1.2,
                block.z + 0.5
            )
            isNew = true
        } else {
            isNew = false
        }

        val active = isActivated(portal)
        consoleItem.setItem(Nms.itemHandle(makeConsoleItem(portal, active))!!)

        if (isNew) {
            consoleFloatingItems[block] = consoleItem
            Nms.spawn(block.world, consoleItem)
        }
    }

    /** Remove the floating console item at the given block, if present. */

    fun removeConsoleItem(block: Block?) {
        val consoleItem = consoleFloatingItems.remove(block)
        consoleItem?.discard()
    }

    /** Iterates all console blocks in [chunk] and applies [consumer] on each. */
    private fun forEachConsoleBlockInChunk(
        chunk: Chunk,
        consumer: Consumer2<Block?, PortalBlockLookup?>
    ) {
        val portalBlocksInChunk = portalBlocksInChunkInWorld[chunk.world.uid] ?: return

        val chunkKey = chunk.chunkKey
        val blockToPortalBlock = portalBlocksInChunk[chunkKey] ?: return

        blockToPortalBlock.forEach { (k: Long?, v: PortalBlockLookup?) ->
            if (v!!.type() == PortalBlock.Type.CONSOLE) {
                consumer.apply(unpackBlockKey(chunk, k!!), v)
            }
        }
    }

    /** Removes floating console items for consoles in a chunk before unload. */

    @EventHandler
    fun onMonitorChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.getChunk()

        // Disable all consoles in this chunk
        forEachConsoleBlockInChunk(
            chunk
        ) { block: Block?, console: PortalBlockLookup? -> removeConsoleItem(block) }
    }

    /** Recreates floating console items for consoles in a chunk after load. */

    @EventHandler
    fun onMonitorChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.getChunk()

        // Enable all consoles in this chunk
        forEachConsoleBlockInChunk(chunk) { block: Block?, console: PortalBlockLookup? ->
            val portal = portalFor(console!!.portalId())
            updateConsoleItem(portal!!, block!!)
        }
    }

    /** Update dynamic map markers for a portal in all configured map integrations. */

    fun updateMarker(portal: Portal) {
        dynmapLayer.updateMarker(portal)
        blueMapLayer.updateMarker(portal)
    }

    /** Remove markers for the given portal id in all configured map integrations. */

    fun removeMarker(portalId: UUID?) {
        dynmapLayer.removeMarker(portalId)
        blueMapLayer.removeMarker(portalId)
    }

    /** Persist portal state when the world is saved. */

    @EventHandler
    fun onSaveWorld(event: WorldSaveEvent) {
        updatePersistentData(event.getWorld())
    }

    /** Handle world load to restore portal state from persistent storage. */

    @EventHandler
    fun onLoadWorld(event: WorldLoadEvent) {
        loadPersistentData(event.getWorld())
    }

    /** Handle world unload to persist/cleanup portal state. */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onUnloadWorld(event: WorldUnloadEvent) {
        // Save data before unloading a world (not called on stop).
        try {
            updatePersistentData(event.world)
        } finally {
            worldStorage.forget(event.world.uid)
        }
    }

    /** Update persistent storage for portals across all worlds. */

    fun updatePersistentData() {
        for (world in server.worlds) {
            updatePersistentData(world)
        }
    }

    /** Initializes components, permissions, and migrations for the module. */
    init {
        registerEntities()

        menus = PortalMenuGroup(this)
        PortalActivator(this)
        PortalBlockProtector(this)
        constructor = PortalConstructor(this)
        PortalTeleporter(this)
        EntityMoveProcessor(this)
        dynmapLayer = PortalDynmapLayer(this)
        blueMapLayer = PortalBlueMapLayer(this)

        // Register admin permission
        adminPermission = Permission(
            "vane." + this.annotationName + ".admin",
            "Allows administration of any portal",
            PermissionDefault.OP
        )
        registerPermission(adminPermission)

        // TODO legacy, remove in v2.
        persistentStorageManager.addMigrationTo(
            2,
            "Portal visibility GROUP_INTERNAL was added. This is a no-op."
        ) { _ -> }
    }

    /**
     * Ensure portals are loaded for worlds that are already loaded when the plugin
     * enables. WorldLoadEvent will handle worlds that load later.
     */
    override fun onModuleEnable() {
        // Schedule on next tick to avoid running during plugin enable timing where
        // some services/world state might not yet be fully available.
        scheduleNextTick {
            if (!enabled()) return@scheduleNextTick
            for (world in server.worlds) {
                loadPersistentData(world)
            }
        }
    }

    /** Load portals from world storage and perform legacy-storage migration for the given world. */

    fun loadPersistentData(world: World) {
        if (!persistentStorageManager.isLoaded || worldStorage.canSave(world.uid)) return
        var converted = false
        try {
            worldStorage.load(world.uid) { converted = readPersistentData(world) }
        } catch (e: Exception) {
            logger.log(java.util.logging.Level.SEVERE,
                "Cannot load portals for world ${world.name} (${world.uid}); keeping its stored data and shutting down", e)
            server.shutdown()
            return
        }

        // Visual initialization is not part of deciding whether stored records were read safely.
        for (chunk in world.loadedChunks) {
            forEachConsoleBlockInChunk(chunk) { block: Block?, console: PortalBlockLookup? ->
                updateConsoleItem(portalFor(console!!.portalId())!!, block!!)
            }
        }
        if (converted) updatePersistentData(world)
    }

    private fun readPersistentData(world: World): Boolean {
        val data = world.persistentDataContainer
        val storagePortalPrefix = "$STORAGE_PORTALS."
        // Decode and validate the entire PDC snapshot before adding anything to the live index.
        val decoded = data.readStoredJsonRecords(storagePortalPrefix) { portalId, json ->
            val portal = requireNotNull(PersistentSerializer.fromJson(Portal::class.java, json))
            require(portal.id() == portalId) { "Portal ID does not match storage key $portalId" }
            require(portal.spawnWorld() == world.uid) { "Portal $portalId belongs to another world" }
            portal
        }
        val pdcPortals = decoded.keys
        val legacy = storagePortals.filterValues { it.spawnWorld() == world.uid }
        legacy.forEach { (id, portal) ->
            require(id == portal.id()) { "Legacy portal ID does not match storage key $id" }
        }
        for (portal in decoded.values + legacy.values) {
            val previous = portals[portal.id()]
            require(previous == null || previous.spawnWorld() == world.uid) {
                "Portal ID ${portal.id()} is already indexed in another world"
            }
        }

        // Rebuild this world's indices on reload, rather than retaining stale entries.
        portals.entries.removeIf { it.value.spawnWorld() == world.uid }
        portalBlocksInChunkInWorld.remove(world.uid)
        decoded.values.forEach(::indexPortal)

        // Only records actually read from this world's PDC supersede their legacy copies.
        val removeFromLegacyStorage = legacy.keys.filter { it in pdcPortals }
        var converted = false
        for ((id, portal) in legacy) {
            if (id in pdcPortals) continue
            indexPortal(portal)
            portal.invalidated = true
            converted = true
        }

        // No legacy entry is retired until all records have been decoded and indexed.
        removeFromLegacyStorage.forEach { storagePortals.remove(it) }
        if (removeFromLegacyStorage.isNotEmpty()) markPersistentStorageDirty()
        logger.info("Loaded ${pdcPortals.size} portals for world ${world.name} (${world.uid})")
        return converted
    }

    /** A partial or skipped load must never be interpreted as deletion or permit overwrites. */
    fun updatePersistentData(world: World) {
        if (!persistentStorageManager.isLoaded) return
        worldStorage.save(world.uid) { writePersistentData(world) }
    }

    private fun writePersistentData(world: World) {
        val data = world.persistentDataContainer
        val storagePortalPrefix = "$STORAGE_PORTALS."

        // Update invalidated portals
        portals.values
            .filter { x -> x.invalidated && x.spawnWorld() == world.uid }
            .forEach { portal ->
                try {
                    val json = PersistentSerializer.toJson(Portal::class.java, portal)
                    data.set(
                        NamespacedKey.fromString(storagePortalPrefix + portal.id().toString())!!,
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().toByteArray()
                    )
                } catch (e: IOException) {
                    logger.log(java.util.logging.Level.SEVERE, "error while serializing persistent data!", e)
                    return@forEach
                }
                portal.invalidated = false
            }

        // Get all currently stored portals.
        val storedPortals = getStoredPortalIds(data, storagePortalPrefix)

        // Remove all portals that no longer exist
        Sets.difference(storedPortals, portals.keys).forEach { id ->
            data.remove(NamespacedKey.fromString(storagePortalPrefix + id.toString())!!)
        }
    }

    /** Extract stored portal ids from the persistent data container using the given storage prefix. */
    private fun getStoredPortalIds(
        data: org.bukkit.persistence.PersistentDataContainer,
        storagePortalPrefix: String
    ): Set<UUID> =
        data.storedUuidsByPrefix(storagePortalPrefix)

    /** Runnable used for delayed disconnection of portal pairs. */
    private inner class PortalDisableRunnable(private val src: Portal?, private val dst: Portal?) : Runnable {
        /** Disconnects the associated portal pair when executed. */
        override fun run() {
            this@Portals.disconnectPortals(src, dst)
        }
    }

    /** Static serializer registration and key helpers for portal storage/indexing. */
    companion object {
        // Add (de-)serializers
        init {
            PersistentSerializer.serializers[Orientation::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as Orientation).name }
            PersistentSerializer.deserializers[Orientation::class.java] =
                PersistentSerializer.Function { x: Any? -> Orientation.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[Portal::class.java] =
                PersistentSerializer.Function { obj: Any? -> Portal.serialize(obj!!) }
            PersistentSerializer.deserializers[Portal::class.java] =
                PersistentSerializer.Function { obj: Any? -> Portal.deserialize(obj!!) }
            PersistentSerializer.serializers[Portal.Visibility::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as Portal.Visibility).name }
            PersistentSerializer.deserializers[Portal.Visibility::class.java] =
                PersistentSerializer.Function { x: Any? -> Portal.Visibility.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[PortalBlock::class.java] =
                PersistentSerializer.Function { obj: Any? -> PortalBlock.serialize(obj!!) }
            PersistentSerializer.deserializers[PortalBlock::class.java] =
                PersistentSerializer.Function { obj: Any? -> PortalBlock.deserialize(obj!!) }
            PersistentSerializer.serializers[PortalBlock.Type::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as PortalBlock.Type).name }
            PersistentSerializer.deserializers[PortalBlock.Type::class.java] =
                PersistentSerializer.Function { x: Any? -> PortalBlock.Type.fromSerialized((x as String?)!!) }
            PersistentSerializer.serializers[PortalBlockLookup::class.java] =
                PersistentSerializer.Function { obj: Any? -> PortalBlockLookup.serialize(obj!!) }
            PersistentSerializer.deserializers[PortalBlockLookup::class.java] =
                PersistentSerializer.Function { obj: Any? -> PortalBlockLookup.deserialize(obj!!) }
            PersistentSerializer.serializers[Style::class.java] =
                PersistentSerializer.Function { obj: Any? -> Style.serialize(obj!!) }
            PersistentSerializer.deserializers[Style::class.java] =
                PersistentSerializer.Function { obj: Any? -> Style.deserialize(obj!!) }
        }

        /** Packs block coordinates into a compact per-chunk key. */
        private fun blockKey(block: Block): Long {
            return ((block.y shl 8) or ((block.x and 0xF) shl 4) or ((block.z and 0xF))).toLong()
        }

        /** Unpack a blockKey back into a block location inside the given chunk. */
        private fun unpackBlockKey(chunk: Chunk, blockKey: Long): Block {
            val y = (blockKey shr 8).toInt()
            val x = ((blockKey shr 4) and 0xFL).toInt()
            val z = (blockKey and 0xFL).toInt()
            return chunk.getBlock(x, y, z)
        }

        /** Root persistent-data key storing per-world serialized portal entries. */
        val STORAGE_PORTALS: NamespacedKey = StorageUtil.namespacedKey("vane_portals", "portals")
    }
}
