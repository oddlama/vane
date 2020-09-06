package org.oddlama.vane.portals;

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

public class Portal {
	public static enum Visibility {
		PUBLIC,
		GROUP,
		PRIVATE;

		public Visibility next() {
			final var next = (ordinal() + 1) % values().length;
			return values()[next];
		}
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
		return Orientation.POSITIVE_X;
	}

	public Location spawn() {
		return null;
	}
}
