package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;
import org.oddlama.vane.core.persistent.PersistentSerializer;


import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBlockLookup;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;
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
	// Add (de-)serializers
	static {
		PersistentSerializer.serializers.put(Portal.class,              Portal::serialize);
		PersistentSerializer.deserializers.put(Portal.class,            Portal::deserialize);
		PersistentSerializer.serializers.put(Portal.Visibility.class,   x -> ((Portal.Visibility)x).name());
		PersistentSerializer.deserializers.put(Portal.Visibility.class, x -> Portal.Visibility.valueOf((String)x));
		PersistentSerializer.serializers.put(PortalBlock.class,         PortalBlock::serialize);
		PersistentSerializer.deserializers.put(PortalBlock.class,       PortalBlock::deserialize);
		PersistentSerializer.serializers.put(PortalBlock.Type.class,    x -> ((PortalBlock.Type)x).name());
		PersistentSerializer.deserializers.put(PortalBlock.Type.class,  x -> PortalBlock.Type.valueOf((String)x));
		PersistentSerializer.serializers.put(PortalBlockLookup.class,   PortalBlockLookup::serialize);
		PersistentSerializer.deserializers.put(PortalBlockLookup.class, PortalBlockLookup::deserialize);
		PersistentSerializer.serializers.put(Orientation.class,         x -> ((Orientation)x).name());
		PersistentSerializer.deserializers.put(Orientation.class,       x -> Orientation.valueOf((String)x));
	}

	// TODO materials
	//@ConfigMaterialMapMapMap(name = "styles")
	//public Map<String, Map<String, Map<String, Material>>> config_styles;

	// Primary storage for all portals (portal_id → portal)
	@Persistent
	private Map<UUID, Portal> storage_portals = new HashMap<>();
	// Primary storage for all portal blocks (world_id → chunk key → block key → portal block)
	@Persistent
	private Map<UUID, Map<Long, Map<Long, PortalBlockLookup>>> storage_portal_blocks_in_chunk_in_world = new HashMap<>();

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

	public Style style(final NamespacedKey key) {
		final var s = styles.get(key);
		if (s == null) {
			log.warning("Encountered invalid style " + key + ", falling back to default style.");
			return styles.get(Style.default_style_key());
		} else {
			return s;
		}
	}

	public void add_portal(final Portal portal) {
		storage_portals.put(portal.id(), portal);
	}

	public void add_portal_block(final UUID portal_id, final PortalBlock portal_block) {
		final var block = portal_block.block();
		final var world_id = block.getWorld().getUID();
		var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(world_id);
		if (portal_blocks_in_chunk == null) {
			portal_blocks_in_chunk = new HashMap<Long, Map<Long, PortalBlockLookup>>();
			storage_portal_blocks_in_chunk_in_world.put(world_id, portal_blocks_in_chunk);
		}

		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			block_to_portal_block = new HashMap<Long, PortalBlockLookup>();
			portal_blocks_in_chunk.put(chunk_key, block_to_portal_block);
		}

		final var block_key = block.getBlockKey();
		block_to_portal_block.put(block_key, portal_block.lookup(portal_id));
	}

	public List<PortalBlock> blocks_for(final UUID portal_id, Predicate<PortalBlock> predicate) {
		final var blocks = new ArrayList<PortalBlock>();

		storage_portal_blocks_in_chunk_in_world.values()
			.forEach(portal_blocks_in_chunk -> {
				portal_blocks_in_chunk.values()
					.forEach(block_to_portal_block -> {
						block_to_portal_block.values()
							.stream()
							.filter(pb -> pb.portal_id().equals(portal_id))
							.filter(predicate)
							.forEachOrdered(blocks::add);
					});
			});

		return blocks;
	}

	public PortalBlockLookup portal_block_for(final Block block) {
		final var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return null;
		}

		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return null;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		return block_to_portal_block.get(block_key);
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
		final var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return false;
		}

		final var chunk_key = Chunk.getChunkKey(block.getX(), block.getZ());
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return false;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		return block_to_portal_block.containsKey(block_key);
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

	public boolean is_activated(final Portal portal) {
		// TODO
		return false;
	}

	public void update_console(final Portal portal, final PortalBlock console, boolean active) {
		// TODO
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
