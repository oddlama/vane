package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.Chunk;
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
import java.util.Map;
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
import org.oddlama.vane.annotation.persistent.Persistent;

@VaneModule(name = "portals", bstats = 8642, config_version = 1, lang_version = 1, storage_version = 1)
public class Portals extends Module<Portals> {
	public static final Material MATERIAL_CONSOLE = Material.ENCHANTING_TABLE;
	public static final Material MATERIAL_PORTAL = Material.END_GATEWAY;

	/*
	 * cross section of portal:
	 *
	 * b1 = leftmost block of portal (bounding box)
	 * b2 = rightmost block of portal (bounding box)
	 * c = console (1 block)
	 * u = maximum distance from console to boundary (blocks in between + 1)
	 * w = maximum width
	 * t = maximum total width
	 *
	 *     u      w       u
	 *   |---|---------|----|
	 *   c   b1        b2   c
	 *
	 * --> PORTAL_AREA_MAX_WIDTH = t - u * 2 - 2;
	 */
	public static final int PORTAL_MAX_WIDTH_IN_CHUNKS = 4; /* minimum is 2, as portals can be built on chunk edges */
	public static final int PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ = 12;
	public static final int PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_Y = 16;
	public static final int PORTAL_AREA_FLOODFILL_MAX_STEPS = 1024;

	public static int PORTAL_AREA_MAX_WIDTH = ((PORTAL_MAX_WIDTH_IN_CHUNKS - 1) * 16) - PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ * 2 - 2;
	public static int PORTAL_AREA_MAX_HEIGHT = 24;
	public static int PORTAL_AREA_MAX_BLOCKS = 64;
	public static int PORTAL_AREA_MAX_BLOCKS_ADMIN = 256;

	//@ConfigMaterialMapMapMap(name = "styles")
	//public Map<String, Map<String, Map<String, Material>>> config_styles;

	@Persistent
	public Map<Long, Map<Long, UUID>> storage_portal_blocks_in_chunk = new HashMap<>();
	@Persistent
	public Map<Long, List<UUID>> storage_portals_in_chunk = new HashMap<>();
	@Persistent
	public Map<UUID, Portal> storage_portals = new HashMap<>();

	// All loaded styles
	public Map<NamespacedKey, Style> styles = new HashMap<>();

	public Portals() {
		new PortalActivator(this);
		new PortalBlockProtector(this);
		new PortalConstructor(this);
		new PortalTeleporter(this);
	}

	@Override
	public void on_config_change() {
		styles.clear();
		final var default_style = Style.default_style();
		styles.put(default_style.key(), default_style);
		// TODO configured styles
	}

	public int max_dim_x(Plane plane) { return plane.x() ? PORTAL_AREA_MAX_WIDTH : 1; }
	public int max_dim_y(Plane plane) { return plane.y() ? PORTAL_AREA_MAX_HEIGHT : 1; }
	public int max_dim_z(Plane plane) { return plane.z() ? PORTAL_AREA_MAX_WIDTH : 1; }

	public Portal portal_for(final Block block) {
		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		final var block_to_portal = storage_portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal == null) {
			return null;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		final var portal_id = block_to_portal.get(block_key);
		if (portal_id == null) {
			return null;
		}

		return storage_portals.get(portal_id);
	}

	public boolean is_portal_block(final Block block) {
		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		final var block_to_portal = storage_portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal == null) {
			return false;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		return block_to_portal.containsKey(block_key);
	}

	public Portal controlled_portal(final Block block) {
		final var root_portal = portal_for(block);
		if (root_portal != null) {
			return root_portal;
		}

		// Find adjacent console blocks in full 3x3x3 cube, which will make this block a controlling block
		for (final var adj : adjacent_blocks_3d(block)) {
			if (adj.getType() == MATERIAL_CONSOLE) {
				final var portal = portal_for(block);
				if (portal != null) {
					return portal;
				}
			}
		}

		return null;
	}

	private void disable_consoles_in_chunk(final Chunk chunk) {
		// TODO
	}

	private void enable_consoles_in_chunk(final Chunk chunk) {
		// TODO
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_unload(final ChunkUnloadEvent event) {
		disable_consoles_in_chunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_load(final ChunkLoadEvent event) {
		enable_consoles_in_chunk(event.getChunk());
	}
}
