package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.portals.event.PortalActivateEvent;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalDeactivateEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;

public class RegionPortalRoleSettingEnforcer extends Listener<Regions> {

	public RegionPortalRoleSettingEnforcer(Context<Regions> context) {
		super(context);
	}

	public boolean check_setting_at(
			final Location location,
			final Player player,
			final RoleSetting setting,
			final boolean check_against
	) {
		final var region = get_module().region_at(location);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_role(player.getUniqueId()).get_setting(setting) == check_against;
	}

	public boolean check_setting_at(
			final Block block,
			final Player player,
			final RoleSetting setting,
			final boolean check_against
	) {
		final var region = get_module().region_at(block);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_role(player.getUniqueId()).get_setting(setting) == check_against;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_activate(final PortalActivateEvent event) {
		if (event.getPlayer() == null) {
			// Activated by redstone -> Always allow. It's the job of the region
			// owner to prevent redstone interactions if a portal shouldn't be activated.
			return;
		}

		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_deactivate(final PortalDeactivateEvent event) {
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_construct(final PortalConstructEvent event) {
		// We have to check all blocks here, because otherwise players
		// could "steal" boundary blocks from unowned regions
		for (final var block : event.getBoundary().all_blocks()) {
			// Portals in regions may only be constructed by region administrators
			if (check_setting_at(block, event.getPlayer(), RoleSetting.ADMIN, false)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_destroy(final PortalDestroyEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// We do NOT have to check all blocks here, because
		// an existing portal with its spawn inside a region
		// that the player controls can be considered proof of authority.
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			// Portals in regions may only be destroyed by region administrators
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_link_console(final PortalLinkConsoleEvent event) {
		if (event.getPortal() != null && event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (event.getPortal() != null && get_module().region_at(event.getPortal().spawn()) != null) {
			// Portals in regions may be administrated by region administrators,
			// not only be the owner
			event.setCancelIfNotOwner(false);
		}

		// Portals in regions may only be administrated by region administrators
		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal if any
		if (
				event.getPortal() != null &&
						check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)
		) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_unlink_console(final PortalUnlinkConsoleEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (get_module().region_at(event.getPortal().spawn()) != null) {
			// Portals in regions may be administrated by region administrators,
			// not only be the owner
			event.setCancelIfNotOwner(false);
		}

		// Portals in regions may only be administrated by region administrators
		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_open_console(final PortalOpenConsoleEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_select_target(final PortalSelectTargetEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// Check permission on source portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on target portal
		if (
				event.getTarget() != null &&
						check_setting_at(event.getTarget().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)
		) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_change_settings(final PortalChangeSettingsEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (get_module().region_at(event.getPortal().spawn()) == null) {
			return;
		}

		// Portals in regions may be administrated by region administrators,
		// not only be the owner
		event.setCancelIfNotOwner(false);

		// Now check if the player has the permission
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
		}
	}
}
