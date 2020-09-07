package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.HashMap;
import java.util.Iterator;
import org.bukkit.event.block.Action;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.Listener;

public class PortalActivator extends Listener<Portals> {
	public PortalActivator(Context<Portals> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_console(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block.getType() != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Abort if the table is not a console
		final var portal = get_module().portal_for(block);
		if (portal == null) {
			return;
		}

		final var player = event.getPlayer();
		if (portal.open_console(player, block)) {
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
			if (!portal.deactivate(player)) {
				event.setUseInteractedBlock(Event.Result.DENY);
				event.setUseItemInHand(Event.Result.DENY);
			}
		} else {
			// Lever is being switched on → activate
			if (!portal.activate(player)) {
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
		portal.activate(null);
    }
}
