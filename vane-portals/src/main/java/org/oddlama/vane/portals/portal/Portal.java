package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import org.oddlama.vane.portals.PortalConstructor;
import org.oddlama.vane.portals.Portals;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.oddlama.vane.util.LazyLocation;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.external.json.JSONObject;

public class Portal {
	public static Object serialize(@NotNull final Object o) throws IOException {
		final var portal = (Portal)o;
		final var json = new JSONObject();
		json.put("id",            to_json(UUID.class,              portal.id));
		json.put("orientation",   to_json(Orientation.class,       portal.orientation));
		json.put("spawn",         to_json(LazyLocation.class,      portal.spawn));
		try {
			json.put("blocks",    to_json(Portal.class.getDeclaredField("blocks"), portal.blocks));
		} catch (NoSuchFieldException e) { throw new RuntimeException("Invalid field. This is a bug.", e); }

		json.put("name",          to_json(String.class,            portal.name));
		json.put("style",         to_json(NamespacedKey.class,     portal.style));
		json.put("icon",          to_json(ItemStack.class,         portal.icon));
		json.put("visibility",    to_json(Visibility.class,        portal.visibility));

		json.put("target_id",     to_json(UUID.class,              portal.target_id));
		json.put("target_locked", to_json(boolean.class,           portal.target_locked));
		return json;
	}

	@SuppressWarnings("unchecked")
	public static Portal deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var portal = new Portal();
		portal.id            = from_json(UUID.class,              json.get("id"));
		portal.orientation   = from_json(Orientation.class,       json.get("orientation"));
		portal.spawn         = from_json(LazyLocation.class,      json.get("spawn"));
		try {
			portal.blocks    = (List<PortalBlock>)from_json(Portal.class.getDeclaredField("blocks"), json.get("blocks"));
		} catch (NoSuchFieldException e) { throw new RuntimeException("Invalid field. This is a bug.", e); }

		portal.name          = from_json(String.class,            json.get("name"));
		portal.style         = from_json(NamespacedKey.class,     json.get("style"));
		portal.icon          = from_json(ItemStack.class,         json.get("icon"));
		portal.visibility    = from_json(Visibility.class,        json.get("visibility"));

		portal.target_id     = from_json(UUID.class,              json.get("target_id"));
		portal.target_locked = from_json(boolean.class,           json.get("target_locked"));
		return portal;
	}

	private UUID id;
	private Orientation orientation;
	private LazyLocation spawn;
	private List<PortalBlock> blocks = new ArrayList<>();

	private String name = "Portal";
	private NamespacedKey style = Style.default_style_key();
	private ItemStack icon = null;
	private Visibility visibility = Visibility.PRIVATE;

	private UUID target_id = null;
	private boolean target_locked = false;

	private Portal() {}

	public Portal(final Orientation orientation, final Location spawn) {
		this.id = UUID.randomUUID();
		this.orientation = orientation;
		this.spawn = new LazyLocation(spawn.clone());
	}

	public UUID id() { return id; }
	public Orientation orientation() { return orientation; }
	public Location spawn() { return spawn.location().clone(); }
	public List<PortalBlock> blocks() { return blocks; }
	public String name() { return name; }
	public void name(String name) { this.name = name; }
	public ItemStack icon() { return icon == null ? null : icon.clone(); }
	public void icon(ItemStack icon) { this.icon = icon; }
	public Visibility visibility() { return visibility; }
	public void visibility(Visibility visibility) { this.visibility = visibility; }
	public UUID target_id() { return target_id; }
	public boolean target_locked() { return target_locked; }

	public @Nullable Portal target(final Portals portals) {
		return portals.portal_for(target_id());
	}

	public boolean activate(final Portals portals, @Nullable final Player player) {
		// TODO send event check cancelled
		portals.connect_portals(this, target(portals));
		return true;
	}

	public boolean deactivate(final Portals portals, @Nullable final Player player) {
		// TODO send event check cancelled
		portals.disconnect_portals(this, target(portals));
		return true;
	}

	public void on_connect(final Portals portals, final Portal target) {
		// Update blocks
		update_blocks(portals);

		// TODO sound
	}

	public void on_disconnect(final Portals portals, final Portal target) {
		// Update blocks
		update_blocks(portals);

		// TODO sound
	}

	public void update_blocks(final Portals portals) {
		final var cur_style = portals.style(style);
		final var active = portals.is_activated(this);
		for (final var portal_block : blocks) {
			portal_block.block().setType(cur_style.material(portal_block.type(), active));
			if (portal_block.type() == PortalBlock.Type.CONSOLE) {
				portals.update_console_item(this, portal_block.block(), active);
			}
		}
	}

	public boolean open_console(final Portals portals, final Player player, final Block console) {
		// Call event
		final var event = new PortalOpenConsoleEvent(player, console, id());
		portals.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		portals.menus.console_menu.create(this, player, console).open(player);
		return true;
	}


	@Override
	public String toString() {
		return "Portal{id = " + id + ", name = " + name + "}";
	}

	public static enum Visibility {
		PUBLIC,
		GROUP,
		PRIVATE;

		public Visibility next() {
			final var next = (ordinal() + 1) % values().length;
			return values()[next];
		}
	}
}
