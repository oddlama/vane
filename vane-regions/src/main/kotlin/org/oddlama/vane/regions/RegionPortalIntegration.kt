package org.oddlama.vane.regions

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.regions.event.RegionPortalRoleSettingEnforcer
import org.oddlama.vane.regions.region.RoleSetting

/** Wires regions permissions into vane-portals callbacks. */
class RegionPortalIntegration(context: Regions, portalsPlugin: Plugin?) {
    /** Registers portal-region callbacks and portal-related setting enforcers. */
    init {
        RegionPortalRoleSettingEnforcer(context)

        if (portalsPlugin is Portals) {
            // Register callback to portals module so portals
            // can find out if two portals are in the same region group
            portalsPlugin.setIsInSameRegionGroupCallback { a: Portal?, b: Portal? ->
                /** First portal to compare. */
                val portalA = a ?: return@setIsInSameRegionGroupCallback false

                /** Second portal to compare. */
                val portalB = b ?: return@setIsInSameRegionGroupCallback false

                /** Region containing `portalA`, if any. */
                val regA = context.regionAt(portalA.spawn())

                /** Region containing `portalB`, if any. */
                val regB = context.regionAt(portalB.spawn())
                if (regA == null || regB == null) {
                    return@setIsInSameRegionGroupCallback regA == regB
                }
                regA.regionGroupId() == regB.regionGroupId()
            }

            portalsPlugin.setPlayerCanUsePortalsInRegionGroupOfCallback {
                player: Player?,
                portal: Portal? ->
                /** Player attempting to use the portal. */
                val user = player ?: return@setPlayerCanUsePortalsInRegionGroupOfCallback false

                /** Target portal being evaluated. */
                val portalRef = portal ?: return@setPlayerCanUsePortalsInRegionGroupOfCallback false

                /** Region containing the portal destination, if one exists. */
                val region =
                    context.regionAt(portalRef.spawn())
                        ?: // No region -> no restriction.
                        return@setPlayerCanUsePortalsInRegionGroupOfCallback true

                /** Region group owning the portal region. */
                val group =
                    region.regionGroup(context)
                        ?: return@setPlayerCanUsePortalsInRegionGroupOfCallback true
                group.getRole(user.uniqueId)?.getSetting(RoleSetting.PORTAL) ?: true
            }
        }
    }
}
