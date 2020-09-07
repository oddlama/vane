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
import java.util.Map;
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
import org.bukkit.NamespacedKey;
import static org.oddlama.vane.util.Util.namespaced_key;
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

public class Style {
	private NamespacedKey key;
	private Map<PortalBlock.Type, Material> active_materials = new HashMap<>();
	private Map<PortalBlock.Type, Material> inactive_materials = new HashMap<>();

	private Style(final NamespacedKey key) {
		this.key = key;
	}

	public NamespacedKey key() {
		return key;
	}

	public Material material(PortalBlock.Type type, boolean active) {
		if (active) {
			return active_materials.get(type);
		} else {
			return inactive_materials.get(type);
		}
	}

	public static Style default_style() {
		final var style = new Style(namespaced_key("vane", "default"));
		style.inactive_materials.put(PortalBlock.Type.ORIGIN,   Material.OBSIDIAN);
		style.inactive_materials.put(PortalBlock.Type.CONSOLE,  Material.ENCHANTING_TABLE);
		style.inactive_materials.put(PortalBlock.Type.BOUNDARY, Material.OBSIDIAN);
		style.inactive_materials.put(PortalBlock.Type.PORTAL,   Material.AIR);
		style.active_materials.put(PortalBlock.Type.ORIGIN,   Material.OBSIDIAN);
		style.active_materials.put(PortalBlock.Type.CONSOLE,  Material.ENCHANTING_TABLE);
		style.active_materials.put(PortalBlock.Type.BOUNDARY, Material.OBSIDIAN);
		style.active_materials.put(PortalBlock.Type.PORTAL,   Material.END_GATEWAY);
		return style;
	}
}
