package org.oddlama.vane.portals;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PortalActivator extends Listener<Portals> {
	public PortalActivator(Context<Portals> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void on_player_interact_console(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (!get_module().portal_console_materials.contains(block.getType())) {
			return;
		}

		// Abort if the table is not a console
		final var portal_block = get_module().portal_block_for(block);
		if (portal_block == null || portal_block.type() != PortalBlock.Type.CONSOLE) {
			return;
		}

		final var player = event.getPlayer();
		final var portal = get_module().portal_for(portal_block);
		if (portal.open_console(get_module(), player, block)) {
			swing_arm(player, event.getHand());
			event.setUseInteractedBlock(Event.Result.DENY);
			event.setUseItemInHand(Event.Result.DENY);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact_lever(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block.getType() != Material.LEVER) {
			return;
		}

		// Get base block the lever is attached to
		final var lever = (Switch)block.getBlockData();
		final BlockFace attached_face;
		switch (lever.getAttachedFace()) {
			default:
			case WALL:    attached_face = lever.getFacing().getOppositeFace(); break;
			case CEILING: attached_face = BlockFace.UP; break;
			case FLOOR:   attached_face = BlockFace.DOWN; break;
		}

		// Find controlled portal
		final var base = block.getRelative(attached_face);
		final var portal = get_module().controlled_portal(base);
		if (portal == null) {
			return;
		}

		// Deactivate portal
		final var player = event.getPlayer();
		if (lever.isPowered()) {
			// Lever is being switched off → deactivate
			if (!portal.deactivate(get_module(), player)) {
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setUseItemInHand(Event.Result.DENY);
			}
		} else {
			// Lever is being switched on → activate
			if (!portal.activate(get_module(), player)) {
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setUseItemInHand(Event.Result.DENY);
			}
		}
	}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_block_redstone(final BlockRedstoneEvent event) {
		// Redstone enable only works on hard-linked get_module().
	    if (event.getOldCurrent() != 0 || event.getNewCurrent() == 0) {
		    return;
		}

	    final var portal = get_module().portal_for(event.getBlock());
	    if (portal == null) {
		    return;
		}

		// TODO setting for "keep on while pulse active" and "toggle on falling/rising edge"
		portal.activate(get_module(), null);
    }
}
