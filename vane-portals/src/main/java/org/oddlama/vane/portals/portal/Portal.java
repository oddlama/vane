package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;
import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.BlockUtil.update_lever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalActivateEvent;
import org.oddlama.vane.portals.event.PortalDeactivateEvent;
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.util.BlockUtil;
import org.oddlama.vane.util.LazyLocation;

public class Portal {

	public static Object serialize(@NotNull final Object o) throws IOException {
		final var portal = (Portal) o;
		final var json = new JSONObject();
		json.put("id", to_json(UUID.class, portal.id));
		json.put("owner", to_json(UUID.class, portal.owner));
		json.put("orientation", to_json(Orientation.class, portal.orientation));
		json.put("spawn", to_json(LazyLocation.class, portal.spawn));
		try {
			json.put("blocks", to_json(Portal.class.getDeclaredField("blocks"), portal.blocks));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}

		json.put("name", to_json(String.class, portal.name));
		json.put("style", to_json(NamespacedKey.class, portal.style));
		json.put("style_override", to_json(Style.class, portal.style_override));
		json.put("icon", to_json(ItemStack.class, portal.icon));
		json.put("visibility", to_json(Visibility.class, portal.visibility));

		json.put("exit_orientation_locked", to_json(boolean.class, portal.exit_orientation_locked));
		json.put("target_id", to_json(UUID.class, portal.target_id));
		json.put("target_locked", to_json(boolean.class, portal.target_locked));
		return json;
	}

	@SuppressWarnings("unchecked")
	public static Portal deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject) o;
		final var portal = new Portal();
		portal.id = from_json(UUID.class, json.get("id"));
		portal.owner = from_json(UUID.class, json.get("owner"));
		portal.orientation = from_json(Orientation.class, json.get("orientation"));
		portal.spawn = from_json(LazyLocation.class, json.get("spawn"));
		try {
			portal.blocks = (List<PortalBlock>) from_json(Portal.class.getDeclaredField("blocks"), json.get("blocks"));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}

		portal.name = from_json(String.class, json.get("name"));
		portal.style = from_json(NamespacedKey.class, json.get("style"));
		portal.style_override = from_json(Style.class, json.get("style_override"));
		if (portal.style_override != null) {
			try {
				portal.style_override.check_valid();
			} catch (RuntimeException e) {
				portal.style_override = null;
			}
		}
		portal.icon = from_json(ItemStack.class, json.get("icon"));
		portal.visibility = from_json(Visibility.class, json.get("visibility"));

		portal.exit_orientation_locked = from_json(boolean.class, json.optString("exit_orientation_locked", "false"));
		portal.target_id = from_json(UUID.class, json.get("target_id"));
		portal.target_locked = from_json(boolean.class, json.get("target_locked"));
		return portal;
	}

	private UUID id;
	private UUID owner;
	private Orientation orientation;
	private LazyLocation spawn;
	private List<PortalBlock> blocks = new ArrayList<>();

	private String name = "Portal";
	private NamespacedKey style = Style.default_style_key();
	private Style style_override = null;
	private ItemStack icon = null;
	private Visibility visibility = Visibility.PRIVATE;

	private boolean exit_orientation_locked = false;
	private UUID target_id = null;
	private boolean target_locked = false;

	// Whether the portal should be saved on next occasion.
	// Not a saved field.
	public boolean invalidated = true;

	private Portal() {}

	public Portal(final UUID owner, final Orientation orientation, final Location spawn) {
		this.id = UUID.randomUUID();
		this.owner = owner;
		this.orientation = orientation;
		this.spawn = new LazyLocation(spawn.clone());
	}

	public UUID id() {
		return id;
	}

	public UUID owner() {
		return owner;
	}

	public Orientation orientation() {
		return orientation;
	}

	public UUID spawn_world() {
		return spawn.world_id();
	}

	public Location spawn() {
		return spawn.location().clone();
	}

	public List<PortalBlock> blocks() {
		return blocks;
	}

	public String name() {
		return name;
	}

	public void name(final String name) {
		this.name = name;
		this.invalidated = true;
	}

	public NamespacedKey style() {
		return style_override == null ? style : null;
	}

	public void style(final Style style) {
		if (style.key() == null) {
			this.style_override = style;
		} else {
			this.style = style.key();
		}
		this.invalidated = true;
	}

	public ItemStack icon() {
		return icon == null ? null : icon.clone();
	}

	public void icon(final ItemStack icon) {
		this.icon = icon;
		this.invalidated = true;
	}

	public Visibility visibility() {
		return visibility;
	}

	public void visibility(final Visibility visibility) {
		this.visibility = visibility;
		this.invalidated = true;
	}

	public boolean exit_orientation_locked() {
		return exit_orientation_locked;
	}

	public void exit_orientation_locked(boolean exit_orientation_locked) {
		this.exit_orientation_locked = exit_orientation_locked;
		this.invalidated = true;
	}

	public UUID target_id() {
		return target_id;
	}

	public void target_id(final UUID target_id) {
		this.target_id = target_id;
		this.invalidated = true;
	}

	public boolean target_locked() {
		return target_locked;
	}

	public void target_locked(boolean target_locked) {
		this.target_locked = target_locked;
		this.invalidated = true;
	}

	public PortalBlock portal_block_for(final Block block) {
		for (final var pb : blocks()) {
			if (pb.block().equals(block)) {
				return pb;
			}
		}
		return null;
	}

	public @Nullable Portal target(final Portals portals) {
		return portals.portal_for(target_id());
	}

	private Set<Block> controlling_blocks() {
		final var controlling_blocks = new HashSet<Block>();
		for (final var pb : blocks()) {
			switch (pb.type()) {
				default:
					break;
				case ORIGIN:
				case BOUNDARY_1:
				case BOUNDARY_2:
				case BOUNDARY_3:
				case BOUNDARY_4:
				case BOUNDARY_5:
					controlling_blocks.add(pb.block());
					break;
				case CONSOLE:
					controlling_blocks.add(pb.block());
					controlling_blocks.addAll(Arrays.asList(adjacent_blocks_3d(pb.block())));
					break;
			}
		}
		return controlling_blocks;
	}

	private void set_controlling_levers(boolean activated) {
		final var controlling_blocks = controlling_blocks();
		final var levers = new HashSet<Block>();
		for (final var b : controlling_blocks()) {
			for (final var f : BlockUtil.BLOCK_FACES) {
				final var l = b.getRelative(f);
				if (l.getType() != Material.LEVER) {
					continue;
				}

				final var lever = (Switch) l.getBlockData();
				final BlockFace attached_face;
				switch (lever.getAttachedFace()) {
					default:
					case WALL:
						attached_face = lever.getFacing().getOppositeFace();
						break;
					case CEILING:
						attached_face = BlockFace.UP;
						break;
					case FLOOR:
						attached_face = BlockFace.DOWN;
						break;
				}

				// Only when attached to a controlling block
				if (!controlling_blocks.contains(l.getRelative(attached_face))) {
					continue;
				}

				levers.add(l);
			}
		}

		for (final var l : levers) {
			final var lever = (Switch) l.getBlockData();
			lever.setPowered(activated);
			l.setBlockData(lever);
			update_lever(l, lever.getFacing());
		}
	}

	public boolean activate(final Portals portals, @Nullable final Player player) {
		if (portals.is_activated(this)) {
			return false;
		}

		final var target = target(portals);
		if (target == null) {
			return false;
		}

		// Call event
		final var event = new PortalActivateEvent(player, this, target);
		portals.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		portals.connect_portals(this, target);
		return true;
	}

	public boolean deactivate(final Portals portals, @Nullable final Player player) {
		if (!portals.is_activated(this)) {
			return false;
		}

		// Call event
		final var event = new PortalDeactivateEvent(player, this);
		portals.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		portals.disconnect_portals(this);
		return true;
	}

	public void on_connect(final Portals portals, final Portal target) {
		// Update blocks
		update_blocks(portals);

		// Activate all controlling levers
		set_controlling_levers(true);

		float sound_volume = (float) portals.config_volume_activation;
		if (sound_volume > 0.0f) {
			// Play sound
			spawn()
				.getWorld()
				.playSound(spawn(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, sound_volume, 0.8f);
		}
	}

	public void on_disconnect(final Portals portals, final Portal target) {
		// Update blocks
		update_blocks(portals);

		// Deactivate all controlling levers
		set_controlling_levers(false);

		float sound_volume = (float) portals.config_volume_deactivation;
		if (sound_volume > 0.0f) {
			// Play sound
			spawn()
				.getWorld()
				.playSound(spawn(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, sound_volume, 0.5f);
		}
	}

	public void update_blocks(final Portals portals) {
		final Style cur_style;
		if (style_override == null) {
			cur_style = portals.style(style);
		} else {
			cur_style = style_override;
		}

		final var active = portals.is_activated(this);
		for (final var portal_block : blocks) {
			final var type = cur_style.material(active, portal_block.type());
			portal_block.block().setType(type);
			if (type == Material.END_GATEWAY) {
				// Disable beam
				final var end_gateway = (EndGateway) portal_block.block().getState(false);
				end_gateway.setAge(200l);
			}
			if (portal_block.type() == PortalBlock.Type.CONSOLE) {
				portals.update_console_item(this, portal_block.block());
			}
		}
	}

	public boolean open_console(final Portals portals, final Player player, final Block console) {
		// Call event
		final var event = new PortalOpenConsoleEvent(player, console, this);
		portals.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() && !player.hasPermission(portals.admin_permission)) {
			return false;
		}

		portals.menus.console_menu.create(this, player, console).open(player);
		return true;
	}

	public Style copy_style(final Portals portals, final NamespacedKey new_key) {
		if (style_override == null) {
			return portals.style(style).copy(new_key);
		}
		return style_override.copy(new_key);
	}

	@Override
	public String toString() {
		return "Portal{id = " + id + ", name = " + name + "}";
	}

	public static enum Visibility {
		PUBLIC,
		GROUP,
		GROUP_INTERNAL,
		PRIVATE;

		public Visibility prev() {
			final int prev;
			if (ordinal() == 0) {
				prev = values().length - 1;
			} else {
				prev = ordinal() - 1;
			}
			return values()[prev];
		}

		public Visibility next() {
			final var next = (ordinal() + 1) % values().length;
			return values()[next];
		}

		public boolean is_transient_target() {
			return this == GROUP || this == PRIVATE;
		}

		public boolean requires_regions() {
			return this == GROUP || this == GROUP_INTERNAL;
		}
	}

	public static class TargetSelectionComparator implements Comparator<Portal> {

		private World world;
		private Vector from;

		public TargetSelectionComparator(final Player player) {
			this.world = player.getLocation().getWorld();
			this.from = player.getLocation().toVector().setY(0.0);
		}

		@Override
		public int compare(final Portal a, final Portal b) {
			boolean a_same_world = world.equals(a.spawn().getWorld());
			boolean b_same_world = world.equals(b.spawn().getWorld());

			if (a_same_world) {
				if (b_same_world) {
					final var a_dist = from.distanceSquared(a.spawn().toVector().setY(0.0));
					final var b_dist = from.distanceSquared(b.spawn().toVector().setY(0.0));
					return Double.compare(a_dist, b_dist);
				} else {
					return -1;
				}
			} else {
				if (b_same_world) {
					return 1;
				} else {
					return a.name().compareToIgnoreCase(b.name());
				}
			}
		}
	}
}
