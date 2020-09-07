package org.oddlama.vane.portals;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;
import org.oddlama.vane.core.persistent.PersistentSerializer;

import org.bukkit.entity.Entity;
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
import org.oddlama.vane.external.json.JSONObject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
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
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.module.Module;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;

public class Portal {
	private static Object serialize(@NotNull final Object o) throws IOException {
		final var portal = (Portal)o;
		final var json = new JSONObject();
		json.put("id",            to_json(UUID.class,          portal.id));
		json.put("name",          to_json(String.class,        portal.name));
		json.put("orientation",   to_json(Orientation.class,   portal.orientation));
		json.put("style",         to_json(NamespacedKey.class, portal.style));
		json.put("spawn",         to_json(Location.class,      portal.spawn));
		json.put("icon",          to_json(ItemStack.class,     portal.icon));
		json.put("visibility",    to_json(Visibility.class,    portal.visibility));
		json.put("target_id",     to_json(UUID.class,          portal.target_id));
		json.put("target_locked", to_json(boolean.class,       portal.target_locked));
		return json;
	}

	private static Portal deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var portal = new Portal();
		portal.id            = from_json(UUID.class,          json.get("id"));
		portal.name          = from_json(String.class,        json.get("name"));
		portal.orientation   = from_json(Orientation.class,   json.get("orientation"));
		portal.style         = from_json(NamespacedKey.class, json.get("style"));
		portal.spawn         = from_json(Location.class,      json.get("spawn"));
		portal.icon          = from_json(ItemStack.class,     json.get("icon"));
		portal.visibility    = from_json(Visibility.class,    json.get("visibility"));
		portal.target_id     = from_json(UUID.class,          json.get("target_id"));
		portal.target_locked = from_json(boolean.class,       json.get("target_locked"));
		return portal;
	}

	// Add (de-)serializer
	static {
		PersistentSerializer.serializers.put(Portal.class,   Portal::serialize);
		PersistentSerializer.deserializers.put(Portal.class, Portal::deserialize);
	}

	private UUID id;
	private String name;
	private Orientation orientation;
	private NamespacedKey style;
	private Location spawn;
	private ItemStack icon;
	private Visibility visibility;

	private UUID target_id;
	private boolean target_locked;

	private Portal() {
	}

	public boolean activate(@Nullable final Player player) {
		// TODO send event check cancelled
		System.out.println("activate");

		// TODO sound
		return true;
	}

	public boolean deactivate(@Nullable final Player player) {
		// TODO send event check cancelled
		System.out.println("deactivate");

		// TODO sound
		return true;
	}

	public boolean link_console(final Player player, final Block console, final Block boundary) {
		// TODO send separate?? event check cancelled
		System.out.println("link");
		return true;
	}

	public PortalBlock block(final Block block) {
		// TODO
		return null;
	}

	public boolean open_console(final Player player, final Block console_block) {
		final var console = block(console_block);
		if (console == null || console.type() != PortalBlock.Type.CONSOLE) {
			return false;
		}

		System.out.println("open console");
		// TODO send separate?? event check cancelled

		//new ConsoleMenu(player, portal, console).open();
		return true;
	}

	public @Nullable Portal target() {
		// TODO
		return null;
	}

	public Orientation orientation() {
		return orientation;
	}

	public Location spawn() {
		return spawn;
	}

	@Override
	public String toString() {
		return "Portal{id = " + id + ", name = " + name + "}";
	}

	public static enum Visibility {
		PUBLIC,
		GROUP,
		PRIVATE;

		// Add (de-)serializer
		static {
			PersistentSerializer.serializers.put(Visibility.class,   x -> ((Visibility)x).name());
			PersistentSerializer.deserializers.put(Visibility.class, x -> Visibility.valueOf((String)x));
		}

		public Visibility next() {
			final var next = (ordinal() + 1) % values().length;
			return values()[next];
		}
	}
}
