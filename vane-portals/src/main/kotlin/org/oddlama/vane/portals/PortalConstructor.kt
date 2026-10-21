package org.oddlama.vane.portals

import java.util.*
import kotlin.math.abs
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.event.PortalConstructEvent
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent
import org.oddlama.vane.portals.portal.*
import org.oddlama.vane.portals.portal.PortalBoundary.Companion.searchAt
import org.oddlama.vane.portals.portal.PortalBoundary.ErrorState
import org.oddlama.vane.util.PlayerUtil

/**
 * Component responsible for detecting portal boundaries and constructing portals from blocks.
 *
 * Handles player interactions during portal construction and links consoles to portals.
 */
class PortalConstructor(context: Context<Portals?>?) : Listener<Portals?>(context) {
    /** Material used for portal console blocks. */
    @JvmField
    @ConfigMaterial(
        def = Material.ENCHANTING_TABLE,
        desc = "The block used to build portal consoles.",
    )
    var configMaterialConsole: Material? = null

    /** Boundary material variant 1. */
    @JvmField
    @ConfigMaterial(
        def = Material.OBSIDIAN,
        desc = "The block used to build the portal boundary. Variation 1.",
    )
    var configMaterialBoundary1: Material? = null

    /** Boundary material variant 2. */
    @JvmField
    @ConfigMaterial(
        def = Material.CRYING_OBSIDIAN,
        desc = "The block used to build the portal boundary. Variation 2.",
    )
    var configMaterialBoundary2: Material? = null

    /** Boundary material variant 3. */
    @JvmField
    @ConfigMaterial(
        def = Material.GOLD_BLOCK,
        desc = "The block used to build the portal boundary. Variation 3.",
    )
    var configMaterialBoundary3: Material? = null

    /** Boundary material variant 4. */
    @JvmField
    @ConfigMaterial(
        def = Material.GILDED_BLACKSTONE,
        desc = "The block used to build the portal boundary. Variation 4.",
    )
    var configMaterialBoundary4: Material? = null

    /** Boundary material variant 5. */
    @JvmField
    @ConfigMaterial(
        def = Material.EMERALD_BLOCK,
        desc = "The block used to build the portal boundary. Variation 5.",
    )
    var configMaterialBoundary5: Material? = null

    /** Material used for the unique portal origin marker. */
    @JvmField
    @ConfigMaterial(
        def = Material.NETHERITE_BLOCK,
        desc = "The block used to build the portal origin.",
    )
    var configMaterialOrigin: Material? = null

    /** Material used for portal interior area blocks. */
    @JvmField
    @ConfigMaterial(def = Material.AIR, desc = "The block used to build the portal area.")
    var configMaterialPortalArea: Material? = null

    /** Maximum horizontal console-to-boundary distance. */
    @ConfigInt(
        def = 12,
        min = 1,
        desc = "Maximum horizontal distance between a console block and the portal.",
    )
    var configConsoleMaxDistanceXz: Int = 0

    /** Maximum vertical console-to-boundary distance. */
    @ConfigInt(
        def = 12,
        min = 1,
        desc = "Maximum vertical distance between a console block and the portal.",
    )
    var configConsoleMaxDistanceY: Int = 0

    /** Maximum flood-fill steps used by boundary detection. */
    @ConfigInt(
        def = 1024,
        min = 256,
        desc =
            "Maximum steps for the floodfill algorithm. This should only be increased if you want really big portals. It's recommended to keep this as low as possible.",
    )
    var configAreaFloodfillMaxSteps: Int = 1024

    /** Maximum portal area width in its bounding box. */
    @ConfigInt(
        def = 24,
        min = 8,
        desc = "Maximum portal area width (bounding box will be measured).",
    )
    var configAreaMaxWidth: Int = 0

    /** Maximum portal area height in its bounding box. */
    @ConfigInt(
        def = 24,
        min = 8,
        desc = "Maximum portal area height (bounding box will be measured).",
    )
    var configAreaMaxHeight: Int = 24

    /** Maximum amount of blocks in the detected portal area. */
    @ConfigInt(def = 128, min = 8, desc = "Maximum total amount of portal area blocks.")
    var configAreaMaxBlocks: Int = 128

    /** Prompt shown after selecting a console and waiting for boundary selection. */
    @LangMessage var langSelectBoundaryNow: TranslatedMessage? = null

    /** Error shown when selected console block uses wrong material. */
    @LangMessage var langConsoleInvalidType: TranslatedMessage? = null

    /** Error shown when console and boundary are in different worlds. */
    @LangMessage var langConsoleDifferentWorld: TranslatedMessage? = null

    /** Error shown when selected console is too far from boundary blocks. */
    @LangMessage var langConsoleTooFarAway: TranslatedMessage? = null

    /** Info shown when a console is already linked. */
    @LangMessage var langConsoleLinked: TranslatedMessage? = null

    /** Error shown when no valid boundary could be found. */
    @LangMessage var langNoBoundaryFound: TranslatedMessage? = null

    /** Error shown when no origin block exists in the boundary. */
    @LangMessage var langNoOrigin: TranslatedMessage? = null

    /** Error shown when multiple origin blocks are found. */
    @LangMessage var langMultipleOrigins: TranslatedMessage? = null

    /** Error shown when the first portal-area block above origin is missing. */
    @LangMessage var langNoPortalBlockAboveOrigin: TranslatedMessage? = null

    /** Error shown when portal area above origin is not tall enough. */
    @LangMessage var langNotEnoughPortalBlocksAboveOrigin: TranslatedMessage? = null

    /** Error shown when boundary dimensions exceed configured limits. */
    @LangMessage var langTooLarge: TranslatedMessage? = null

    /** Error shown when spawn space is too small/invalid. */
    @LangMessage var langTooSmallSpawn: TranslatedMessage? = null

    /** Error shown when portal area block count exceeds configured limit. */
    @LangMessage var langTooManyPortalAreaBlocks: TranslatedMessage? = null

    /** Error shown when portal area is obstructed. */
    @LangMessage var langPortalAreaObstructed: TranslatedMessage? = null

    /** Error shown when detected boundary intersects existing portals. */
    @LangMessage var langIntersectsExistingPortal: TranslatedMessage? = null

    /** Error shown when construction is denied by integration hooks. */
    @LangMessage var langBuildRestricted: TranslatedMessage? = null

    /** Error shown when console linking is denied by integration hooks. */
    @LangMessage var langLinkRestricted: TranslatedMessage? = null

    /** Error shown when selecting a target that is already connected elsewhere. */
    @LangMessage var langTargetAlreadyConnected: TranslatedMessage? = null

    /** Error shown when source portal usage is denied. */
    @LangMessage var langSourceUseRestricted: TranslatedMessage? = null

    /** Error shown when target portal usage is denied. */
    @LangMessage var langTargetUseRestricted: TranslatedMessage? = null

    /** All materials accepted as boundary/origin during detection. */
    private val portalBoundaryBuildMaterials: MutableSet<Material?> = HashSet()

    /** Per-player pending console awaiting boundary click. */
    private val pendingConsole = HashMap<UUID?, Block?>()

    /** Rebuilds derived material sets whenever configuration changes. */
    public override fun onConfigChange() {
        portalBoundaryBuildMaterials.clear()
        portalBoundaryBuildMaterials.add(configMaterialBoundary1)
        portalBoundaryBuildMaterials.add(configMaterialBoundary2)
        portalBoundaryBuildMaterials.add(configMaterialBoundary3)
        portalBoundaryBuildMaterials.add(configMaterialBoundary4)
        portalBoundaryBuildMaterials.add(configMaterialBoundary5)
        portalBoundaryBuildMaterials.add(configMaterialOrigin)
    }

    /** Returns max x dimension allowed for portals in [plane]. */
    fun maxDimX(plane: Plane) = if (plane.x()) configAreaMaxWidth else 1

    /** Returns max y dimension allowed for portals in [plane]. */
    fun maxDimY(plane: Plane) = if (plane.y()) configAreaMaxHeight else 1

    /** Returns max z dimension allowed for portals in [plane]. */
    fun maxDimZ(plane: Plane) = if (plane.z()) configAreaMaxWidth else 1

    /** Stores a pending console selection for [player] and notifies on change. */
    private fun rememberNewConsole(player: Player, consoleBlock: Block): Boolean {
        val changed = consoleBlock != pendingConsole[player.uniqueId]
        // Add consoleBlock as pending console
        pendingConsole[player.uniqueId] = consoleBlock
        if (changed) {
            langSelectBoundaryNow!!.send(player)
        }
        return changed
    }

    /** Validates whether [console] can be linked to the detected [boundary]. */
    // Wrapper that performs an existence/permission check (checkOnly = true)
    private fun canLinkConsoleCheck(
        player: Player,
        boundary: PortalBoundary,
        console: Block,
    ): Boolean {
        return canLinkConsole(player, boundary.allBlocks(), console, null, true)
    }

    /** Validates whether [console] can be linked into an existing [portal]. */
    // Wrapper that links an existing portal (checkOnly = false)
    private fun canLinkConsoleLink(player: Player, portal: Portal, console: Block): Boolean {
        // Gather all portal blocks that aren't consoles
        val blocks =
            portal
                .blocks()
                .orEmpty()
                .filter { pb -> pb.type() != PortalBlock.Type.CONSOLE }
                .mapTo(ArrayList()) { pb -> pb.block()!! }
        return canLinkConsole(player, blocks, console, portal, false)
    }

    /** Shared console-link validation path for checks and actual linking. */
    private fun canLinkConsole(
        player: Player,
        blocks: MutableList<Block>,
        console: Block,
        existingPortal: Portal?,
        checkOnly: Boolean,
    ): Boolean {
        // Check a console block type
        if (console.type != configMaterialConsole) {
            langConsoleInvalidType!!.send(player)
            return false
        }

        // Check world
        if (console.world != blocks[0].world) {
            langConsoleDifferentWorld!!.send(player)
            return false
        }

        // Check distance
        var foundValidBlock = false
        for (block in blocks) {
            if (
                abs(console.x - block.x) <= configConsoleMaxDistanceXz &&
                    abs(console.y - block.y) <= configConsoleMaxDistanceY &&
                    abs(console.z - block.z) <= configConsoleMaxDistanceXz
            ) {
                foundValidBlock = true
                break
            }
        }

        if (!foundValidBlock) {
            langConsoleTooFarAway!!.send(player)
            return false
        }

        // Call event: PortalLinkConsoleEvent expects MutableList<Block?>?, so create a nullable
        // list copy
        val eventBlocks = blocks.mapTo(ArrayList<Block?>()) { it }
        val event = PortalLinkConsoleEvent(player, console, eventBlocks, checkOnly, existingPortal)
        module!!.server.pluginManager.callEvent(event)
        if (event.isCancelled() && !player.hasPermission(module!!.adminPermission)) {
            langLinkRestricted!!.send(player)
            return false
        }

        return true
    }

    /** Links [console] into [portal] after validation and refreshes portal blocks. */
    private fun linkConsole(player: Player, console: Block, portal: Portal): Boolean {
        if (!canLinkConsoleLink(player, portal, console)) {
            return false
        }

        // Add portal block
        module!!.addNewPortalBlock(portal, createPortalBlock(console))

        // Update block blocks
        portal.updateBlocks(module!!)
        return true
    }

    /** Searches for a valid portal boundary near [block], optionally notifying [player]. */
    private fun findBoundary(player: Player?, block: Block): PortalBoundary? {
        val boundary = searchAt(this, block)
        if (boundary == null) {
            langNoBoundaryFound!!.send(player)
            return null
        }

        // Check for error
        when (boundary.errorState()) {
            ErrorState.NONE -> {}
            ErrorState.NO_ORIGIN -> {
                langNoOrigin!!.send(player)
                return null
            }

            ErrorState.MULTIPLE_ORIGINS -> {
                langMultipleOrigins!!.send(player)
                return null
            }

            ErrorState.NO_PORTAL_BLOCK_ABOVE_ORIGIN -> {
                langNoPortalBlockAboveOrigin!!.send(player)
                return null
            }

            ErrorState.NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN -> {
                langNotEnoughPortalBlocksAboveOrigin!!.send(player)
                return null
            }

            ErrorState.TOO_LARGE_X -> {
                langTooLarge!!.send(player, "§6x")
                return null
            }

            ErrorState.TOO_LARGE_Y -> {
                langTooLarge!!.send(player, "§6y")
                return null
            }

            ErrorState.TOO_LARGE_Z -> {
                langTooLarge!!.send(player, "§6z")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_X -> {
                langTooSmallSpawn!!.send(player, "§6x")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_Y -> {
                langTooSmallSpawn!!.send(player, "§6y")
                return null
            }

            ErrorState.TOO_SMALL_SPAWN_Z -> {
                langTooSmallSpawn!!.send(player, "§6z")
                return null
            }

            ErrorState.PORTAL_AREA_OBSTRUCTED -> {
                langPortalAreaObstructed!!.send(player)
                return null
            }

            ErrorState.TOO_MANY_PORTAL_AREA_BLOCKS -> {
                langTooManyPortalAreaBlocks!!.send(
                    player,
                    "§6" + boundary.portalAreaBlocks()!!.size,
                    "§6$configAreaMaxBlocks",
                )
                return null
            }
        }

        if (boundary.intersectsExistingPortal(this)) {
            langIntersectsExistingPortal!!.send(player)
            return null
        }

        return boundary
    }

    /** Returns whether [material] matches any configured boundary material variant. */
    fun isTypePartOfBoundary(material: Material?) =
        material == configMaterialBoundary1 ||
            material == configMaterialBoundary2 ||
            material == configMaterialBoundary3 ||
            material == configMaterialBoundary4 ||
            material == configMaterialBoundary5

    /** Returns whether [material] is part of boundary or origin definitions. */
    fun isTypePartOfBoundaryOrOrigin(material: Material?): Boolean {
        return material == configMaterialOrigin || isTypePartOfBoundary(material)
    }

    /** Validates all conditions required before constructing a portal. */
    private fun checkConstructionConditions(
        player: Player,
        console: Block,
        boundaryBlock: Block,
        checkOnly: Boolean,
    ): PortalBoundary? {
        if (module!!.isPortalBlock(boundaryBlock)) {
            module!!
                .log
                .severe(
                    "constructPortal() was called on a boundary that already belongs to a portal! This is a bug."
                )
            return null
        }

        // Search for valid portal boundary
        val boundary = findBoundary(player, boundaryBlock) ?: return null

        // Check portal construct event
        val event = PortalConstructEvent(player, boundary, checkOnly)
        module!!.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            langBuildRestricted!!.send(player)
            return null
        }

        // Check console distance and build permission
        if (!canLinkConsoleCheck(player, boundary, console)) {
            return null
        }

        return boundary
    }

    /** Maps world [block] material to a semantic [PortalBlock] type. */
    private fun createPortalBlock(block: Block): PortalBlock {
        val type: PortalBlock.Type
        var mat = block.type
        // treat cave air and void air as normal air
        if (mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            mat = Material.AIR
        }
        when (mat) {
            configMaterialConsole -> {
                type = PortalBlock.Type.CONSOLE
            }

            configMaterialBoundary1 -> {
                type = PortalBlock.Type.BOUNDARY1
            }

            configMaterialBoundary2 -> {
                type = PortalBlock.Type.BOUNDARY2
            }

            configMaterialBoundary3 -> {
                type = PortalBlock.Type.BOUNDARY3
            }

            configMaterialBoundary4 -> {
                type = PortalBlock.Type.BOUNDARY4
            }

            configMaterialBoundary5 -> {
                type = PortalBlock.Type.BOUNDARY5
            }

            configMaterialOrigin -> {
                type = PortalBlock.Type.ORIGIN
            }

            configMaterialPortalArea -> {
                type = PortalBlock.Type.PORTAL
            }

            else -> {
                module!!
                    .log
                    .warning(
                        "Invalid block type '" +
                            mat +
                            "' encountered in portal block creation. Assuming boundary variant 1."
                    )
                type = PortalBlock.Type.BOUNDARY1
            }
        }
        return PortalBlock(block, type)
    }

    /** Runs interactive portal construction flow from [console] and [boundaryBlock]. */
    private fun constructPortal(player: Player, console: Block, boundaryBlock: Block): Boolean {
        if (checkConstructionConditions(player, console, boundaryBlock, true) == null) {
            return false
        }

        // Show name chooser
        val enterNameMenu = module!!.menus?.enterNameMenu
        if (enterNameMenu == null) {
            module!!.log.warning("EnterNameMenu is not available")
            return false
        }
        enterNameMenu
            .create(
                player,
                Function2 { p: Player?, name: String? ->
                    // Re-check conditions, as someone could have changed blocks. This
                    // prevents this race condition.
                    val boundary =
                        checkConstructionConditions(p!!, console, boundaryBlock, false)
                            ?: return@Function2 Menu.ClickResult.ERROR

                    // Determine orientation
                    val orientation =
                        Orientation.from(
                            boundary.plane()!!,
                            boundary.originBlock()!!,
                            console,
                            player.location,
                        )

                    // Construct portal
                    val portal = Portal(p.uniqueId, orientation, boundary.spawn()!!)
                    portal.name(name)
                    module!!.addNewPortal(portal)

                    // Add portal blocks
                    for (block in boundary.allBlocks()) {
                        module!!.addNewPortalBlock(portal, createPortalBlock(block))
                    }

                    // Link console
                    linkConsole(p, console, portal)

                    // Force update storage now, as a precaution.
                    module!!.updatePersistentData()

                    // Update portal blocks once
                    portal.updateBlocks(module!!)
                    Menu.ClickResult.SUCCESS
                },
            )
            .open(player)

        return true
    }

    /** Captures sneak-right-clicked console blocks as pending construction anchors. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractConsole(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        if (block.type != configMaterialConsole) {
            return
        }

        // Abort if the console belongs to another portal already.
        if (module!!.isPortalBlock(block)) {
            return
        }

        // TODO portal stone as item instead of shifting?
        // Only if player sneak-right-clicks the console
        val player = event.getPlayer()
        if (!player.isSneaking || event.hand != EquipmentSlot.HAND) {
            return
        }

        rememberNewConsole(player, block)
        PlayerUtil.swingArm(player, event.hand!!)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }

    /** Completes portal construction/linking when a boundary block is selected. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerInteractBoundary(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        val portal: Portal? = module!!.portalFor(block)
        val type = block.type
        if (portal == null && !portalBoundaryBuildMaterials.contains(type)) {
            return
        }

        // Break if no console is pending
        val player = event.getPlayer()
        val console = pendingConsole.remove(player.uniqueId) ?: return

        if (portal == null) {
            if (constructPortal(player, console, block)) {
                PlayerUtil.swingArm(player, event.hand!!)
            }
        } else {
            if (linkConsole(player, console, portal)) {
                PlayerUtil.swingArm(player, event.hand!!)
            }
        }

        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }
}
