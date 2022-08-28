package org.oddlama.vane.portals;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PortalBlockProtector extends Listener<Portals> {

	public PortalBlockProtector(Context<Portals> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_block_break(final BlockBreakEvent event) {
		// Prevent breaking of portal blocks
		if (get_module().is_portal_block(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_block_place(final BlockPlaceEvent event) {
		// Prevent (re-)placing of portal blocks
		if (get_module().is_portal_block(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_entity_explode(final EntityExplodeEvent event) {
		// Prevent explosions from removing portal blocks
		event.blockList().removeIf(block -> get_module().is_portal_block(block));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_entity_change_block(final EntityChangeBlockEvent event) {
		// Prevent entities from changing portal blocks
		if (get_module().is_portal_block(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_block_piston_extend(final BlockPistonExtendEvent event) {
		// Prevent pistons from moving portal blocks
		for (final var block : event.getBlocks()) {
			if (get_module().is_portal_block(block)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_block_piston_retract(final BlockPistonRetractEvent event) {
		// Prevent pistons from moving portal blocks
		for (final var block : event.getBlocks()) {
			if (get_module().is_portal_block(block)) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
