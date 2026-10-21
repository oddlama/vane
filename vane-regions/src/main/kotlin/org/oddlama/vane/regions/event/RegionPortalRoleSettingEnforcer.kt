package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.event.*
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.RoleSetting

/** Enforces region role permissions for vane-portals interactions. */
class RegionPortalRoleSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    /** Owning regions module instance. */
    private val regions: Regions
        get() = requireNotNull(module)

    /** Checks whether a role setting at a location matches the expected value. */
    fun checkSettingAt(
        location: Location,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean,
    ): Boolean {
        /** Region at the queried location. */
        val region = regions.regionAt(location) ?: return false

        /** Region group owning that region. */
        val group = region.regionGroup(regions) ?: return false

        /** Effective player role inside the region group. */
        val role = group.getRole(player.uniqueId) ?: return false
        return role.getSetting(setting) == checkAgainst
    }

    /** Checks whether a role setting at a block matches the expected value. */
    fun checkSettingAt(
        block: Block,
        player: Player,
        setting: RoleSetting,
        checkAgainst: Boolean,
    ): Boolean {
        /** Region containing the queried block. */
        val region = regions.regionAt(block) ?: return false

        /** Region group owning that region. */
        val group = region.regionGroup(regions) ?: return false

        /** Effective player role inside the region group. */
        val role = group.getRole(player.uniqueId) ?: return false
        return role.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels portal activation when `PORTAL` usage is denied. */
    fun onPortalActivate(event: PortalActivateEvent) {
        val player =
            event.player
                ?: run {
                    // Activated by redstone -> Always allow. It's the job of the region
                    // owner to prevent redstone interactions if a portal shouldn't be activated.
                    return
                }
        val portal = event.portal ?: return

        if (checkSettingAt(portal.spawn(), player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels portal deactivation when `PORTAL` usage is denied. */
    fun onPortalDeactivate(event: PortalDeactivateEvent) {
        val portal = event.portal ?: return
        val player = event.player ?: return
        if (checkSettingAt(portal.spawn(), player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts portal construction to region admins for all boundary blocks. */
    fun onPortalConstruct(event: PortalConstructEvent) {
        val boundary = event.boundary ?: return
        val player = event.player ?: return
        // We have to check all blocks here, because otherwise players
        // could "steal" boundary blocks from unowned regions
        for (block in boundary.allBlocks()) {
            // Portals in regions may only be constructed by region administrators
            if (checkSettingAt(block, player, RoleSetting.ADMIN, false)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts portal destruction to owners or region admins. */
    fun onPortalDestroy(event: PortalDestroyEvent) {
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        // We do NOT have to check all blocks here, because
        // an existing portal with its spawn inside a region
        // that the player controls can be considered proof of authority.
        if (checkSettingAt(event.portal.spawn(), event.player, RoleSetting.ADMIN, false)) {
            // Portals in regions may only be destroyed by region administrators
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    /** Restricts linking consoles to portals based on region admin permissions. */
    fun onPortalLinkConsole(event: PortalLinkConsoleEvent) {
        val player = event.player
        val portal = event.portal
        if (portal != null && portal.owner() == player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (portal != null && regions.regionAt(portal.spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false)
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        val console = event.console ?: return
        if (checkSettingAt(console, player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal if any
        if (portal != null && checkSettingAt(portal.spawn(), player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    /** Restricts unlinking consoles from portals based on region admin permissions. */
    fun onPortalUnlinkConsole(event: PortalUnlinkConsoleEvent) {
        val player = event.player
        val portal = event.portal
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (regions.regionAt(portal.spawn()) != null) {
            // Portals in regions may be administrated by region administrators,
            // not only be the owner
            event.setCancelIfNotOwner(false)
        }

        // Portals in regions may only be administrated by region administrators
        // Check permission on console
        val console = event.console ?: return
        if (checkSettingAt(console, player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal
        if (checkSettingAt(portal.spawn(), player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts opening portal consoles based on `PORTAL` permissions. */
    fun onPortalOpenConsole(event: PortalOpenConsoleEvent) {
        val portal = event.portal ?: return
        val player = event.player ?: return
        if (portal.owner() == player.uniqueId) {
            // Owner may always use their portals
            return
        }

        // Check permission on console
        val console = event.console ?: return
        if (checkSettingAt(console, player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on portal
        if (checkSettingAt(portal.spawn(), player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts selecting portal targets based on `PORTAL` permissions. */
    fun onPortalSelectTarget(event: PortalSelectTargetEvent) {
        val portal = event.portal ?: return
        val player = event.player ?: return
        if (portal.owner() == player.uniqueId) {
            // Owner may always use their portals
            return
        }

        // Check permission on source portal
        if (checkSettingAt(portal.spawn(), player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
            return
        }

        // Check permission on target portal
        val target = event.target
        if (target != null && checkSettingAt(target.spawn(), player, RoleSetting.PORTAL, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    /** Restricts portal settings changes to owners or region admins. */
    fun onPortalChangeSettings(event: PortalChangeSettingsEvent) {
        if (event.portal.owner() == event.player.uniqueId) {
            // Owner may always use their portals
            return
        }

        if (regions.regionAt(event.portal.spawn()) == null) {
            return
        }

        // Portals in regions may be administrated by region administrators,
        // not only be the owner
        event.setCancelIfNotOwner(false)

        // Now check if the player has the permission
        if (checkSettingAt(event.portal.spawn(), event.player, RoleSetting.ADMIN, false)) {
            event.isCancelled = true
        }
    }
}
