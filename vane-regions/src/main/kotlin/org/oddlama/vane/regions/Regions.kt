package org.oddlama.vane.regions

import com.google.common.collect.Sets
import net.minecraft.core.BlockPos
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.core.persistent.WorldStorageGuard
import org.oddlama.vane.core.persistent.readStoredJsonRecords
import org.oddlama.vane.core.persistent.storedUuidsByPrefix
import org.oddlama.vane.regions.event.RegionEnvironmentSettingEnforcer
import org.oddlama.vane.regions.event.RegionRoleSettingEnforcer
import org.oddlama.vane.regions.event.RegionSelectionListener
import org.oddlama.vane.regions.menu.RegionGroupMenuTag
import org.oddlama.vane.regions.menu.RegionMenuGroup
import org.oddlama.vane.regions.menu.RegionMenuTag
import org.oddlama.vane.regions.region.*
import org.oddlama.vane.regions.region.Role.RoleType
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil
import java.io.IOException
import java.util.*
import java.util.logging.Level
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@VaneModule(name = "regions", bstats = 8643, configVersion = 4, langVersion = 3, storageVersion = 1)
/**
 * Main module entry point for region ownership, permissions, pricing, and visualization.
 */
class Regions : Module<Regions?>() {
    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in x direction.")
            /**
             * Minimum allowed region size on the X axis.
             */
    var configMinRegionExtentX: Int = 0

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in y direction.")
            /**
             * Minimum allowed region size on the Y axis.
             */
    var configMinRegionExtentY: Int = 0

    @ConfigInt(def = 4, min = 1, desc = "Minimum region extent in z direction.")
            /**
             * Minimum allowed region size on the Z axis.
             */
    var configMinRegionExtentZ: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in x direction.")
            /**
             * Maximum allowed region size on the X axis.
             */
    var configMaxRegionExtentX: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in y direction.")
            /**
             * Maximum allowed region size on the Y axis.
             */
    var configMaxRegionExtentY: Int = 0

    @ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in z direction.")
            /**
             * Maximum allowed region size on the Z axis.
             */
    var configMaxRegionExtentZ: Int = 0

    @ConfigBoolean(def = false, desc = "Use economy via VaultAPI as currency provider.")
            /**
             * Uses Vault-backed economy instead of material currency when enabled.
             */
    var configEconomyAsCurrency: Boolean = false

    @ConfigBoolean(
        def = false,
        desc = "Enable this to prevent players without the container permission from being able to view chests."
    )
            /**
             * Blocks chest/container viewing for players lacking container permissions.
             */
    var configProhibitViewingContainers: Boolean = false

    @ConfigInt(
        def = 0,
        min = -1,
        desc = "The amount of decimal places the costs will be rounded to. If set to -1, it will round to the amount of decimal places specified by your economy plugin. If set to 0, costs will simply be rounded up to the nearest integer."
    )
            /**
             * Decimal precision used when formatting and rounding economy costs.
             */
    var configEconomyDecimalPlaces: Int = 0

    @ConfigMaterial(
        def = Material.DIAMOND,
        desc = "The currency material for regions. The alternative option to an economy plugin."
    )
            /**
             * Material used as fallback currency when economy integration is disabled.
             */
    var configCurrency: Material? = null

    @ConfigDouble(
        def = 2.0,
        min = 0.0,
        desc = "The base amount of currency required to buy an area equal to one chunk (256 blocks)."
    )
            /**
             * Base horizontal purchase cost for one chunk-sized area.
             */
    var configCostXzBase: Double = 0.0

    @ConfigDouble(
        def = 1.15,
        min = 1.0,
        desc = "The multiplicator determines how much the cost increases for each additional 16 blocks of height. A region of height h will cost multiplicator^(h / 16.0) * base_amount. Rounding is applied at the end."
    )
            /**
             * Vertical cost multiplier applied per 16 blocks of selected height.
             */
    var configCostYMultiplicator: Double = 0.0

    // Primary persistent storage for all regions (`region.id -> region`).
    @Persistent
    /**
     * Legacy/global region storage used during migration and persistence updates.
     */
    private val storageRegions: MutableMap<UUID?, Region> = HashMap<UUID?, Region>()

    /**
     * Active in-memory index of loaded regions by id.
     */
    private val regions: MutableMap<UUID?, Region?> = HashMap()

    private val worldStorage = WorldStorageGuard()

    // Primary persistent storage for all region groups (`regionGroup.id -> regionGroup`).
    @Persistent
    /**
     * Persistent storage for region groups.
     */
    private val storageRegionGroups: MutableMap<UUID?, RegionGroup> = HashMap<UUID?, RegionGroup>()

    // Maps player uuid to default region group id used for newly created regions.
    @Persistent
    /**
     * Persistent default region-group mapping per player.
     */
    private val storageDefaultRegionGroup: MutableMap<UUID?, UUID?> = HashMap()

    // Per-chunk lookup cache (`worldId -> chunkKey -> possible regions`).
    /**
     * Fast spatial index for region lookups.
     */
    private val regionsInChunkInWorld: MutableMap<UUID?, MutableMap<Long?, MutableList<Region>>> =
        HashMap<UUID?, MutableMap<Long?, MutableList<Region>>>()

    // Current selection state per player while using region selection mode.
    /**
     * In-memory selection extents keyed by player UUID.
     */
    private val regionSelections: MutableMap<UUID, RegionSelection> = HashMap<UUID, RegionSelection>()

    @LangMessage
            /**
             * Message shown when region-selection mode starts.
             */
    var langStartRegionSelection: TranslatedMessage? = null

    // Permission that bypasses normal region administration restrictions.
    /**
     * Administrative override permission for region management operations.
     */
    val adminPermission: Permission

    /**
     * Menu group entry point for region-related UI flows.
     */
    var menus: RegionMenuGroup? = RegionMenuGroup(this)

    /**
     * Dynmap integration layer wrapper.
     */
    var dynmapLayer: RegionDynmapLayer

    /**
     * BlueMap integration layer wrapper.
     */
    var blueMapLayer: RegionBlueMapLayer

    /**
     * Active economy delegate when Vault integration is enabled.
     */
    var economy: RegionEconomyDelegate? = null

    /**
     * Indicates whether `vane-portals` integration is active.
     */
    var vanePortalsAvailable: Boolean = false

    /**
     * Performs delayed startup initialization that depends on loaded plugins.
     */
    fun delayedOnEnable() {
        if (configEconomyAsCurrency) {
            if (!setupEconomy()) {
                configEconomyAsCurrency = false
            }
        }
    }

    /**
     * Initializes Vault economy integration.
     *
     * @return `true` when economy setup succeeds.
     */
    private fun setupEconomy(): Boolean {
        module!!.log.info("Enabling economy integration")

        val serviceIoPlugin: Plugin? = module!!.server.pluginManager.getPlugin("ServiceIO")
        if (serviceIoPlugin == null) {
            module!!.log.severe(
                "Economy was selected as the currency provider, but the ServiceIO plugin wasn't found! Falling back to material currency."
            )
            return false
        }

        economy = RegionEconomyDelegate(this)
        return economy!!.setup(serviceIoPlugin)
    }

    /**
     * Enables optional integrations and starts periodic selection visualization.
     */
    override fun onModuleEnable() {
        val portalsPlugin: Plugin? = module!!.server.pluginManager.getPlugin("vane-portals")
        if (portalsPlugin != null) {
            RegionPortalIntegration(this, portalsPlugin)
            vanePortalsAvailable = true
        }

        scheduleNextTick {
            if (!enabled()) return@scheduleNextTick
            // WorldLoadEvent may already have fired before this module was enabled.
            for (world in server.worlds) loadPersistentData(world)
            this.delayedOnEnable()
        }
        // Refresh region-selection visualization once per second.
        scheduleTaskTimer({ this.visualizeSelections() }, 1L, 20L)
    }

    /**
     * Returns all loaded regions whose world is currently available.
     */
    fun allRegions(): MutableCollection<Region?> {
        return regions.values
            .filter { region ->
                val worldId = region?.extent()?.world() ?: return@filter false
                server.getWorld(worldId) != null
            }
            .toMutableList()
    }

    /**
     * Returns all persisted region groups.
     */
    fun allRegionGroups(): MutableCollection<RegionGroup?> {
        // Call sites expect a nullable element collection, so widen element type explicitly.
        return storageRegionGroups.values.map { it as RegionGroup? }.toMutableList()
    }

    /**
     * Starts region selection mode for the given player.
     */
    fun startRegionSelection(player: Player) {
        regionSelections[player.uniqueId] = RegionSelection(this)
        requireNotNull(langStartRegionSelection).send(player)
    }

    /**
     * Cancels region selection mode for the given player.
     */
    fun cancelRegionSelection(player: Player) {
        regionSelections.remove(player.uniqueId)
    }

    /**
     * Returns whether the player is currently in region selection mode.
     */
    fun isSelectingRegion(player: Player): Boolean {
        return regionSelections.containsKey(player.uniqueId)
    }

    /**
     * Returns the active region selection for the player.
     */
    fun getRegionSelection(player: Player): RegionSelection {
        return requireNotNull(regionSelections[player.uniqueId])
    }

    /**
     * Visualizes one selection-box edge with neutral and validity-colored particles.
     */
    private fun visualizeEdge(world: World, c1: BlockPos, c2: BlockPos, valid: Boolean) {
        // Particle offsets are Gaussian distributed, so scale standard deviation empirically.
        val mx = (c1.getX() + c2.getX()) / 2.0 + 0.5
        val my = (c1.getY() + c2.getY()) / 2.0 + 0.5
        val mz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5
        var dx = abs(c1.getX() - c2.getX()).toDouble()
        var dy = abs(c1.getY() - c2.getY()).toDouble()
        var dz = abs(c1.getZ() - c2.getZ()).toDouble()
        val len = dx + dy + dz
        val count = min(VISUALIZE_MAX_PARTICLES, (VISUALIZE_PARTICLES_PER_BLOCK * len).toInt())

        // Compensate for Gaussian particle spread.
        dx *= VISUALIZE_STDDEV_COMPENSATION
        dy *= VISUALIZE_STDDEV_COMPENSATION
        dz *= VISUALIZE_STDDEV_COMPENSATION

        // Spawn base outline particles.
        world.spawnParticle<Any?>(
            Particle.END_ROD,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0,  // speed
            null,  // data
            true
        ) // force

        // Spawn colored particles indicating selection validity.
        world.spawnParticle<DustOptions?>(
            Particle.DUST,
            mx,
            my,
            mz,
            count,
            dx,
            dy,
            dz,
            0.0,  // speed
            if (valid) VISUALIZE_DUST_VALID else VISUALIZE_DUST_INVALID,  // data
            true
        ) // force
    }

    /**
     * Visualizes a rectangular face by rendering its four edges.
     */
    private fun visualizeFace(world: World, c1: BlockPos, c2: BlockPos, c3: BlockPos, c4: BlockPos, valid: Boolean) {
        visualizeEdge(world, c1, c2, valid)
        visualizeEdge(world, c2, c3, valid)
        visualizeEdge(world, c3, c4, valid)
        visualizeEdge(world, c4, c1, valid)
    }

    /**
     * Renders selection bounding boxes for all online players currently selecting regions.
     */
    private fun visualizeSelections() {
        for (selectionOwner in regionSelections.keys) {
            val selection = regionSelections[selectionOwner] ?: continue

            // Get player for selection
            val offlinePlayer = server.getOfflinePlayer(selectionOwner)
            if (!offlinePlayer.isOnline) {
                continue
            }
            val player = offlinePlayer.player

            // Both blocks are set
            val primary = selection.primary ?: continue
            val secondary = selection.secondary ?: continue

            // World match
            if (primary.world != secondary.world) {
                continue
            }

            // Extent can be visualized. Prepare parameters.
            val world = primary.world
            // Check if selection is valid
            val onlinePlayer = player ?: continue
            val valid = selection.isValid(onlinePlayer)

            val lx = min(primary.x, secondary.x)
            val ly = min(primary.y, secondary.y)
            val lz = min(primary.z, secondary.z)
            val hx = max(primary.x, secondary.x)
            val hy = max(primary.y, secondary.y)
            val hz = max(primary.z, secondary.z)

            // Corners
            val a = BlockPos(lx, ly, lz)
            val b = BlockPos(hx, ly, lz)
            val c = BlockPos(hx, hy, lz)
            val d = BlockPos(lx, hy, lz)
            val e = BlockPos(lx, ly, hz)
            val f = BlockPos(hx, ly, hz)
            val g = BlockPos(hx, hy, hz)
            val h = BlockPos(lx, hy, hz)

            // Visualize each edge
            visualizeFace(world, a, b, c, d, valid)
            visualizeFace(world, e, f, g, h, valid)
            visualizeEdge(world, a, e, valid)
            visualizeEdge(world, b, f, valid)
            visualizeEdge(world, c, g, valid)
            visualizeEdge(world, d, h, valid)
        }
    }

    /**
     * Adds a new region group to persistent storage.
     */
    fun addRegionGroup(group: RegionGroup) {
        storageRegionGroups[group.id()] = group
        markPersistentStorageDirty()
    }

    /**
     * Returns whether the region group can be removed safely.
     */
    fun canRemoveRegionGroup(group: RegionGroup): Boolean {
        // Returns true if this region group is unused and can be removed.

        // If this region group is the fallback default group, it is permanent!

        if (storageDefaultRegionGroup.containsValue(group.id())) {
            return false
        }

        // If any region uses this group, we can't remove it.
        return regions.values.none { region -> region?.regionGroupId() == group.id() }
    }

    /**
     * Removes an unused region group and closes dependent open menus.
     */
    fun removeRegionGroup(group: RegionGroup) {
        // Assert that this region group is unused.
        if (!canRemoveRegionGroup(group)) {
            return
        }

        // Remove a region group from storage
        if (storageRegionGroups.remove(group.id()) == null) {
            // Was already removed
            return
        }

        markPersistentStorageDirty()

        // Close and taint all related open menus
        module!!.core!!.menuManager!!.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is RegionGroupMenuTag &&
                (menu.tag() as RegionGroupMenuTag).regionGroupId() == group.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }
    }

    /**
     * Returns a region group by id.
     */
    fun getRegionGroup(regionGroup: UUID?): RegionGroup? {
        if (regionGroup == null) return null
        return storageRegionGroups[regionGroup]
    }

    /**
     * Creates a region from the player's current valid selection and charges its cost.
     *
     * @return `true` if the region was created successfully.
     */
    fun createRegionFromSelection(player: Player, name: String?): Boolean {
        val selection = getRegionSelection(player)
        if (!selection.isValid(player)) {
            return false
        }

        // Take currency items / withdraw economy
        val price = selection.price()
        if (configEconomyAsCurrency) {
            if (price > 0) {
                val success = economy!!.withdraw(player, price)
                if (!success) {
                    log.warning(
                        "Player " +
                                player +
                                " tried to create region '" +
                                name +
                                "' (cost " +
                                price +
                                ") but the economy plugin failed to withdraw."
                    )
                    return false
                }
            }
        } else {
            val map: MutableMap<ItemStack?, Int> = HashMap()
            map[ItemStack(configCurrency!!)] = price.toInt()
            if (price > 0 && !PlayerUtil.takeItems(player, map)) {
                return false
            }
        }

        val defRegionGroup = getOrCreateDefaultRegionGroup(player)
        val region = Region(name, player.uniqueId, selection.extent(), defRegionGroup.id())
        addNewRegion(region)
        cancelRegionSelection(player)
        return true
    }

    /**
     * Returns the active currency label used in UI and messages.
     */
    fun currencyString(): String? = if (configEconomyAsCurrency) {
        economy?.currencyNamePlural()
    } else {
        configCurrency.toString().lowercase(Locale.getDefault())
    }

    /**
     * Adds and indexes a newly created region.
     */
    fun addNewRegion(region: Region) {
        region.invalidated = true
        // Index region for fast lookup
        indexRegion(region)
    }

    /**
     * Removes a region from storage, indexes, markers, and dependent menus.
     */
    fun removeRegion(region: Region) {
        // Remove region from storage
        if (regions.remove(region.id()) == null) {
            // Was already removed
            return
        }

        // Force update storage now, as a precaution.
        updatePersistentData()

        // Close and taint all related open menus
        module!!.core!!.menuManager!!.forEachOpen { player: Player?, menu: Menu? ->
            if (menu!!.tag() is RegionMenuTag &&
                (menu.tag() as RegionMenuTag).regionId() == region.id()
            ) {
                menu.taint()
                menu.close(player!!)
            }
        }

        // Remove a region from index
        indexRemoveRegion(region)

        // Remove map marker
        removeMarker(region.id()!!)
    }

    /**
     * Updates all map-provider markers for the given region.
     */
    fun updateMarker(region: Region) {
        dynmapLayer.updateMarker(region)
        blueMapLayer.updateMarker(region)
    }

    /**
     * Removes all map-provider markers for the given region id.
     */
    fun removeMarker(regionId: UUID) {
        dynmapLayer.removeMarker(regionId)
        blueMapLayer.removeMarker(regionId)
    }

    /**
     * Indexes a region into per-chunk lookup maps and refreshes map markers.
     */
    private fun indexRegion(region: Region) {
        regions[region.id()] = region

        // Adds the region to the lookup map at all intersecting chunks
        val min = region.extent()!!.min()
        val max = region.extent()!!.max()

        val worldId = min!!.world.uid
        val regionsInChunk =
            regionsInChunkInWorld.computeIfAbsent(worldId) { _ -> HashMap<Long?, MutableList<Region>>() }

        val minChunk = min.chunk
        val maxChunk = max!!.chunk

        // Iterate all the chunks which intersect the region
        for (cx in minChunk.x..maxChunk.x) {
            for (cz in minChunk.z..maxChunk.z) {
                val chunkKey = Chunk.getChunkKey(cx, cz)
                val possibleRegions = regionsInChunk.computeIfAbsent(chunkKey) { _ -> ArrayList<Region>() }
                possibleRegions.add(region)
            }
        }

        // Create map marker
        updateMarker(region)
    }

    /**
     * Removes a region from all intersecting per-chunk lookup entries.
     */
    private fun indexRemoveRegion(region: Region) {
        // Removes the region from the lookup map at all intersecting chunks
        val min = region.extent()!!.min()
        val max = region.extent()!!.max()

        val worldId = min!!.world.uid
        val regionsInChunk = regionsInChunkInWorld[worldId] ?: return

        val minChunk = min.chunk
        val maxChunk = max!!.chunk

        // Iterate all the chunks which intersect the region
        for (cx in minChunk.x..maxChunk.x) {
            for (cz in minChunk.z..maxChunk.z) {
                val chunkKey = Chunk.getChunkKey(cx, cz)
                val possibleRegions = regionsInChunk[chunkKey] ?: continue
                possibleRegions.remove(region)
            }
        }
    }

    /**
     * Finds the first region in a chunk matching the given predicate.
     */
    private fun findRegionInChunk(worldId: UUID, chunkKey: Long, inside: (Region) -> Boolean): Region? {
        val regionsInChunk = regionsInChunkInWorld[worldId] ?: return null
        val possibleRegions = regionsInChunk[chunkKey] ?: return null
        return possibleRegions.firstOrNull { inside(it) }
    }

    /**
     * Returns the region containing the given location, if any.
     */
    fun regionAt(loc: Location): Region? =
        findRegionInChunk(loc.world.uid, loc.chunk.chunkKey) { it.extent()?.isInside(loc) == true }

    /**
     * Returns the region containing the given block, if any.
     */
    fun regionAt(block: Block): Region? =
        findRegionInChunk(block.world.uid, block.chunk.chunkKey) { it.extent()?.isInside(block) == true }

    /**
     * Returns whether the player may administrate the given region group.
     */
    fun mayAdministrate(player: Player, group: RegionGroup): Boolean {
        return player.uniqueId == group.owner() || group.getRole(player.uniqueId)?.getSetting(RoleSetting.ADMIN) == true
    }

    /**
     * Returns whether the player may administrate the given region.
     */
    fun mayAdministrate(player: Player, region: Region): Boolean {
        return player.uniqueId == region.owner() || player.hasPermission(adminPermission)
    }

    /**
     * Returns the owner's default region group, creating it when missing.
     */
    fun getOrCreateDefaultRegionGroup(owner: Player): RegionGroup {
        val ownerId = owner.uniqueId
        val regionGroupId = storageDefaultRegionGroup[ownerId]
        if (regionGroupId != null) {
            getRegionGroup(regionGroupId)?.let { return it }
        }

        // Create and save owner's default group
        val regionGroup = RegionGroup("[default] " + owner.name, ownerId)
        addRegionGroup(regionGroup)

        // Set group as the default
        storageDefaultRegionGroup[ownerId] = regionGroup.id()
        markPersistentStorageDirty()

        return regionGroup
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            /**
             * Clears pending selection state when a player disconnects.
             */
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Remove pending selection
        cancelRegionSelection(event.getPlayer())
    }

    @EventHandler
            /**
             * Persists region data for the world being saved.
             */
    fun onSaveWorld(event: WorldSaveEvent) {
        updatePersistentData(event.getWorld())
    }

    @EventHandler
            /**
             * Loads region data when a world is loaded.
             */
    fun onLoadWorld(event: WorldLoadEvent) {
        loadPersistentData(event.getWorld())
    }

    /** Persists world region data before a non-cancelled world unload. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onUnloadWorld(event: WorldUnloadEvent) {
        try {
            updatePersistentData(event.world)
        } finally {
            worldStorage.forget(event.world.uid)
        }
    }

    init {
        roleOverrides = RegionGlobalRoleOverrides(this)
        environmentOverrides = RegionGlobalEnvironmentOverrides(this)

        org.oddlama.vane.regions.commands.Region(this)

        RegionEnvironmentSettingEnforcer(this)
        RegionRoleSettingEnforcer(this)

        RegionSelectionListener(this)
        dynmapLayer = RegionDynmapLayer(this)
        blueMapLayer = RegionBlueMapLayer(this)

        // Register administrative override permission.
        adminPermission = Permission(
            "vane." + module!!.annotationName + ".admin",
            "Allows administration of any region",
            PermissionDefault.OP
        )
        module!!.registerPermission(adminPermission)
    }

    /**
     * Returns all stored region UUIDs under the provided persistent-data prefix.
     */
    private fun storedRegionIds(data: org.bukkit.persistence.PersistentDataContainer, prefix: String): Set<UUID> =
        data.storedUuidsByPrefix(prefix)

    /**
     * Loads region state for a specific world and migrates legacy entries when needed.
     */
    fun loadPersistentData(world: World) {
        if (!persistentStorageManager.isLoaded || worldStorage.canSave(world.uid)) return
        var converted = false
        try {
            worldStorage.load(world.uid) { converted = readPersistentData(world) }
        } catch (e: Exception) {
            log.log(Level.SEVERE,
                "Cannot load regions for world ${world.name} (${world.uid}); keeping its stored data and shutting down", e)
            // Continuing with a partial protection index would fail open.
            server.shutdown()
            return
        }
        if (converted) updatePersistentData(world)
    }

    private fun readPersistentData(world: World): Boolean {
        val data = world.persistentDataContainer
        val storageRegionPrefix = "$STORAGE_REGIONS."
        // Decode the complete PDC snapshot before indexing; never skip wrong-typed values.
        val decoded = data.readStoredJsonRecords(storageRegionPrefix) { regionId, json ->
            val region = requireNotNull(PersistentSerializer.fromJson(Region::class.java, json))
            require(region.id() == regionId) { "Region ID does not match storage key $regionId" }
            require(region.extent()?.world() == world.uid) { "Region $regionId belongs to another world" }
            region
        }
        val pdcRegions = decoded.keys
        val legacy = storageRegions.filterValues { requireNotNull(it.extent()).world() == world.uid }
        legacy.forEach { (id, region) ->
            require(id == region.id()) { "Legacy region ID does not match storage key $id" }
        }
        for (region in decoded.values + legacy.values) {
            val previous = regions[region.id()]
            require(previous == null || previous.extent()?.world() == world.uid) {
                "Region ID ${region.id()} is already indexed in another world"
            }
        }

        // Region chunk lookups contain lists: re-indexing without clearing duplicates protection.
        // A region whose PDC write failed exists only in memory; keep it so the next save retries it.
        val unsaved = regions.values.filterNotNull().filter {
            it.extent()?.world() == world.uid && it.invalidated && it.id() !in pdcRegions && it.id() !in legacy
        }

        regions.entries.removeIf { it.value?.extent()?.world() == world.uid }
        regionsInChunkInWorld.remove(world.uid)
        decoded.values.forEach(::indexRegion)
        unsaved.forEach(::indexRegion)

        val removeFromLegacyStorage = legacy.keys.filter { it in pdcRegions }
        var converted = false
        for ((id, region) in legacy) {
            if (id in pdcRegions) continue
            indexRegion(region)
            region.invalidated = true
            converted = true
        }

        removeFromLegacyStorage.forEach { storageRegions.remove(it) }
        if (removeFromLegacyStorage.isNotEmpty()) markPersistentStorageDirty()
        log.info("Loaded ${pdcRegions.size} regions for world ${world.name} (${world.uid})")
        return converted
    }

    /** Persists region state for all loaded worlds. */
    fun updatePersistentData() {
        for (world in server.worlds) {
            updatePersistentData(world)
        }
    }

    /** A partial or skipped load must never be interpreted as deletion or permit overwrites. */
    fun updatePersistentData(world: World) {
        if (!persistentStorageManager.isLoaded) return
        worldStorage.save(world.uid) { writePersistentData(world) }
    }

    private fun writePersistentData(world: World) {
        val data = world.persistentDataContainer
        val storageRegionPrefix = "$STORAGE_REGIONS."

        // Update invalidated regions
        regions
            .values
            .filter { region -> region?.invalidated == true && region.extent()?.world() == world.uid }
            .forEach { region ->
                val regionRef = region ?: return@forEach
                try {
                    val json = PersistentSerializer.toJson(Region::class.java, regionRef)
                    val regionId = regionRef.id() ?: return@forEach
                    val key = NamespacedKey.fromString(storageRegionPrefix + regionId) ?: return@forEach
                    data.set(
                        key,
                        PersistentDataType.BYTE_ARRAY,
                        json.toString().toByteArray()
                    )
                } catch (e: IOException) {
                    log.log(Level.SEVERE, "error while serializing persistent data!", e)
                    return@forEach
                }
                regionRef.invalidated = false
            }

        // Get all currently stored regions.
        val storedRegions = storedRegionIds(data, storageRegionPrefix)

        // Remove all regions that no longer exist
        Sets.difference<UUID?>(storedRegions, regions.keys)
            .forEach { id ->
                val key = NamespacedKey.fromString(storageRegionPrefix + id.toString()) ?: return@forEach
                data.remove(key)
            }
    }

    /**
     * Flushes region data before module shutdown.
     */
    override fun onModuleDisable() {
        try {
            updatePersistentData()
            super.onModuleDisable()
        } finally {
            worldStorage.clear()
        }
    }

    companion object {
        //
        //                                                  ┌───────────────────────┐
        // ┌────────────┐  is   ┌───────────────┐         ┌───────────────────────┐ |  belongs to
        // ┌─────────────────┐
        // |  Player 1  | ────> | [Role] Admin  | ───┬──> | [RegionGroup] Default |─┘ <───┬─────── |
        // [Region] MyHome |
        // └────────────┘       └───────────────┘    |    └───────────────────────┘       |
        // └─────────────────┘
        //                                           |                                    |
        // ┌────────────┐  in   ┌───────────────┐    |                                    |
        // ┌─────────────────────┐
        // |  Player 2  | ────> | [Role] Friend | ───┤ (are roles of)                     └─────── |
        // [Region] Drecksloch |
        // └────────────┘       └───────────────┘    |
        // └─────────────────────┘
        //                                           |
        // ┌────────────┐  in   ┌───────────────┐    |
        // | Any Player | ────> | [Role] Others | ───┘
        // └────────────┘       └───────────────┘
        // Add (de-)serializers
        init {
            PersistentSerializer.serializers[EnvironmentSetting::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as EnvironmentSetting).name }
            PersistentSerializer.deserializers[EnvironmentSetting::class.java] =
                PersistentSerializer.Function { x: Any? -> EnvironmentSetting.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[RoleSetting::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as RoleSetting).name }
            PersistentSerializer.deserializers[RoleSetting::class.java] =
                PersistentSerializer.Function { x: Any? -> RoleSetting.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[Role::class.java] =
                PersistentSerializer.Function { obj: Any? -> Role.serialize(obj!!) }
            PersistentSerializer.deserializers[Role::class.java] =
                PersistentSerializer.Function { obj: Any? -> Role.deserialize(obj!!) }
            PersistentSerializer.serializers[RoleType::class.java] =
                PersistentSerializer.Function { x: Any? -> (x as RoleType).name }
            PersistentSerializer.deserializers[RoleType::class.java] =
                PersistentSerializer.Function { x: Any? -> RoleType.valueOf((x as String?)!!) }
            PersistentSerializer.serializers[RegionGroup::class.java] =
                PersistentSerializer.Function { obj: Any? -> RegionGroup.serialize(obj!!) }
            PersistentSerializer.deserializers[RegionGroup::class.java] =
                PersistentSerializer.Function { obj: Any? -> RegionGroup.deserialize(obj!!) }
            PersistentSerializer.serializers[Region::class.java] =
                PersistentSerializer.Function { obj: Any? -> Region.serialize(obj!!) }
            PersistentSerializer.deserializers[Region::class.java] =
                PersistentSerializer.Function { obj: Any? -> Region.deserialize(obj!!) }
            PersistentSerializer.serializers[RegionExtent::class.java] =
                PersistentSerializer.Function { obj: Any? -> RegionExtent.serialize(obj!!) }
            PersistentSerializer.deserializers[RegionExtent::class.java] =
                PersistentSerializer.Function { obj: Any? -> RegionExtent.deserialize(obj!!) }
        }

        /**
         * Global role-setting overrides applied across region checks.
         */
        var roleOverrides: RegionGlobalRoleOverrides? = null

        /**
         * Global environment-setting overrides applied across region checks.
         */
        var environmentOverrides: RegionGlobalEnvironmentOverrides? = null

        /**
         * Hard cap for spawned visualization particles per rendered edge.
         */
        private const val VISUALIZE_MAX_PARTICLES = 20000

        /**
         * Particle density used when rendering selection edges.
         */
        private const val VISUALIZE_PARTICLES_PER_BLOCK = 12

        /**
         * Gaussian spread compensation factor for edge visualization.
         */
        private const val VISUALIZE_STDDEV_COMPENSATION = 0.25

        /**
         * Dust color used for invalid selections.
         */
        private val VISUALIZE_DUST_INVALID = DustOptions(Color.fromRGB(230, 60, 11), 1.0f)

        /**
         * Dust color used for valid selections.
         */
        private val VISUALIZE_DUST_VALID = DustOptions(Color.fromRGB(120, 220, 60), 1.0f)

        /**
         * Base key namespace for region persistent storage entries.
         */
        val STORAGE_REGIONS: NamespacedKey = StorageUtil.namespacedKey("vane_regions", "regions")
    }
}
