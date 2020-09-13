package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.BlockUtil.unpack;
import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.Util.namespaced_key;
import net.md_5.bungee.api.chat.BaseComponent;
import static org.oddlama.vane.util.Nms.register_entity;
import static org.oddlama.vane.util.Nms.spawn;
import static org.oddlama.vane.util.Nms.item_handle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.v1_16_R2.EntityTypes;
import net.minecraft.server.v1_16_R2.EnumCreatureType;
import java.util.ArrayList;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;

import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;
import java.util.UUID;
import java.util.function.Predicate;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.menu.PortalMenuGroup;

import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.PortalBoundary;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBlockLookup;
import org.oddlama.vane.portals.portal.Style;
import org.oddlama.vane.portals.portal.Portal;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
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
import org.oddlama.vane.portals.entity.FloatingItem;

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

	@LangMessage public TranslatedMessage lang_console_display_active;
	@LangMessage public TranslatedMessage lang_console_display_inactive;
	@LangMessage public TranslatedMessage lang_console_no_target;

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

	public PortalMenuGroup menus;

	// Track console items
	private final HashMap<Block, FloatingItem> console_floating_items = new HashMap<>();

	public Portals() {
		register_entities();

		menus = new PortalMenuGroup(this);
		new PortalActivator(this);
		new PortalBlockProtector(this);
		new PortalConstructor(this);
		new PortalTeleporter(this);
	}

	@SuppressWarnings("unchecked")
	private void register_entities() {
		// Register entity
		register_entity(namespace(), "floating_item", EntityTypes.Builder.a(FloatingItem::new, EnumCreatureType.MISC).a(0.0f, 0.0f));
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

	public void remove_portal(final Portal portal) {
		storage_portals.remove(portal.id());

		// TODO replace target id everywhere,
		// and update all changed portals that are loaded in next tick
		// i.e. update_if_loaded() function.
	}

	public void add_portal(final Portal portal) {
		storage_portals.put(portal.id(), portal);
	}

	public void remove_portal_block(final Portal portal, final PortalBlock portal_block) {
		// Remove from portal
		portal.blocks().remove(portal_block);

		// Remove from acceleration structure
		// TODO
	}

	public void add_portal_block(final Portal portal, final PortalBlock portal_block) {
		// Add to portal
		portal.blocks().add(portal_block);

		// Add to acceleration structure
		final var block = portal_block.block();
		final var world_id = block.getWorld().getUID();
		var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(world_id);
		if (portal_blocks_in_chunk == null) {
			portal_blocks_in_chunk = new HashMap<Long, Map<Long, PortalBlockLookup>>();
			storage_portal_blocks_in_chunk_in_world.put(world_id, portal_blocks_in_chunk);
		}

		final var chunk_key = block.getChunk().getChunkKey();
		var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			block_to_portal_block = new HashMap<Long, PortalBlockLookup>();
			portal_blocks_in_chunk.put(chunk_key, block_to_portal_block);
		}

		final var block_key = block.getBlockKey();
		block_to_portal_block.put(block_key, portal_block.lookup(portal.id()));
	}

	public PortalBlockLookup portal_block_for(final Block block) {
		final var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return null;
		}

		final var chunk_key = block.getChunk().getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return null;
		}

		// getBlockKey stores more information than the location in the chunk,
		// but this is okay here as we only need a unique key for every block in the chunk.
		final var block_key = block.getBlockKey();
		return block_to_portal_block.get(block_key);
	}

	public Portal portal_for(@NotNull final PortalBlockLookup block) {
		return storage_portals.get(block.portal_id());
	}

	public Portal portal_for(@Nullable final UUID uuid) {
		return storage_portals.get(uuid);
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

		final var chunk_key = block.getChunk().getChunkKey();
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

	private ItemStack make_console_item(final Portal portal, boolean active) {
		final var target = portal.target(this);
		ItemStack item = null;

		// Try to use target portal's block
		if (target != null) {
			item = target.icon();
		}

		// Fallback item
		if (item == null) {
			item = ExtendedMaterial.from(namespaced_key("vane", "decoration_end_portal_orb")).item();
		}

		final var target_name = target == null ? lang_console_no_target.str() : target.name();
		final BaseComponent display_name;
		if (active) {
			display_name = lang_console_display_active.format("§5" + target_name);
		} else {
			display_name = lang_console_display_inactive.format("§7" + target_name);
		}

		return name_item(item, display_name);
	}

	public void update_console_item(final Portal portal, final Block block, boolean active) {
		var console_item = console_floating_items.get(block);
		final boolean is_new;
		if (console_item == null) {
			console_item = new FloatingItem(block.getWorld(), block.getX() + 0.5, block.getY() + 1.2, block.getZ() + 0.5);
			is_new = true;
		} else {
			is_new = false;
		}

		console_item.setItemStack(item_handle(make_console_item(portal, active)));

		if (is_new) {
			console_floating_items.put(block, console_item);
			spawn(block.getWorld(), console_item);
		}
	}

	public void remove_console_item(final Block block) {
		final var console_item = console_floating_items.remove(block);
		if (console_item != null) {
			console_item.die();
		}
	}

	private void for_each_console_block_in_chunk(final Chunk chunk, final Consumer2<Block, PortalBlockLookup> consumer) {
		final var portal_blocks_in_chunk = storage_portal_blocks_in_chunk_in_world.get(chunk.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return;
		}

		final var chunk_key = chunk.getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return;
		}

		block_to_portal_block.forEach((k, v) -> {
			if (v.type() == PortalBlock.Type.CONSOLE) {
				final var block = unpack(chunk, k);
				consumer.apply(block, v);
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_unload(final ChunkUnloadEvent event) {
		final var chunk = event.getChunk();

		// Disable all consoles in this chunk
		for_each_console_block_in_chunk(chunk, (block, console) -> remove_console_item(block));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_load(final ChunkLoadEvent event) {
		final var chunk = event.getChunk();

		// Enable all consoles in this chunk
		for_each_console_block_in_chunk(chunk, (block, console) -> {
			final var portal = portal_for(console.portal_id());
			final var active = is_activated(portal);
			update_console_item(portal, block, active);
		});
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
