package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
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
import org.oddlama.vane.core.Listener;

public class PortalConstructor extends Listener<Portals> {
	public static final Material MATERIAL_BUILD_BOUNDARY = Material.OBSIDIAN;
	// TODO custom origin block?
	public static final Material MATERIAL_BUILD_ORIGIN = Material.NETHERITE_BLOCK;

	private Portals portals;
	private HashMap<UUID, Block> pending_console = new HashMap<>();

	public PortalConstructor(Portals portals) {
		super(portals);
		this.portals = portals;
	}

	private void begin_portal_construction(final Player player, final Block console_block) {
		// Add console_block as pending console
		pending_console.put(player.getUniqueId(), console_block);
		// TODO player.sendMessage(PortalsConfiguration.PORTAL_SELECT_BOUNDARY_NOW.get());
		player.sendMessage("click boundary");
	}

	private void construct_portal(final Player player, final Block console, final Block boundary_block) {
		if (portals.is_portal_block(boundary_block)) {
			get_module().log.severe("construct_portal() was called on a boundary that already belongs to a portal! This is a bug.");
			return;
		}

		System.out.println("construct");
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_construct_portal(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block.getType() != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Abort if the console belongs to another portal already.
		if (portals.is_portal_block(block)) {
			return;
		}

		// TODO portal stone as item instead of shifting?
		// Only if player sneak-right-clicks the console
		final var player = event.getPlayer();
		if (!player.isSneaking() || event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		begin_portal_construction(player, block);
		swing_arm(player, event.getHand());
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact_boundary(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		final var type = block.getType();
		if (type != MATERIAL_BUILD_BOUNDARY || type != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Break if no console is pending
		final var player = event.getPlayer();
		final var console = pending_console.get(player.getUniqueId());
		if (console == null) {
			return;
		}

		final var portal = portals.portal_for(block);
		if (portal == null) {
			construct_portal(player, console, block);
		} else {
			if (portal.link_console(player, console, block)) {
				swing_arm(player, event.getHand());
			}
		}

		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}
}
