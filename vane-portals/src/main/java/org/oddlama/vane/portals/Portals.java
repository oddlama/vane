package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "portals", bstats = 8642, config_version = 1, lang_version = 1, storage_version = 1)
public class Portals extends Module<Portals> {
	//@ConfigMaterialMapMapMap(name = "styles")
	//public Map<String, Map<String, Map<String, Material>>> config_styles;

	@Persistent
	public Map<Long, Map<Long, PortalBlock>> storage_portal_blocks_in_chunk = new HashMap<>();
	@Persistent
	public Map<Long, List<UUID>> storage_portals_in_chunk = new HashMap<>();
	@Persistent
	public Map<UUID, Portal> storage_portals = new HashMap<>();

	// All loaded styles
	public Map<NamespacedKey, Style> styles = new HashMap<>();
	// Sets for all possible materials for a specific portal block type
	public Set<Material> portal_area_materials = new HashSet<>();
	public Set<Material> portal_console_materials = new HashSet<>();
	public Set<Material> portal_boundary_materials = new HashSet<>();

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

		// TODO acquire from styles
		portal_area_materials.clear();
		portal_area_materials.add(Material.END_GATEWAY);
		portal_console_materials.clear();
		portal_console_materials.add(Material.ENCHANTING_TABLE);
		portal_boundary_materials.clear();
		portal_boundary_materials.add(Material.OBSIDIAN);
	}

	public PortalBlock portal_block_for(final Block block) {
		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		final var block_to_portal = storage_portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal == null) {
			return null;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		return block_to_portal.get(block_key);
	}

	public Portal portal_for(@NotNull final PortalBlock block) {
		return storage_portals.get(block.portal_id());
	}

	public Portal portal_for(final Block block) {
		final var portal_block = portal_block_for(block);
		if (portal_block == null) {
			return null;
		}

		return portal_for(portal_block);
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
			if (portal_console_materials.contains(adj.getType())) {
				final var portal_block = portal_block_for(block);
				if (portal_block != null && portal_block.type() == PortalBlock.Type.CONSOLE) {
					return portal_for(portal_block);
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

	private static class PortalDisableRunnable implements Runnable {
		private UUID src_id;
		private UUID dst_id;

		public PortalDisableRunnable(UUID src_id, UUID dst_id) {
			this.src_id = src_id;
			this.dst_id = dst_id;
		}

		@Override
		public void run() {
			// TODO
		}
	}
}
