package org.oddlama.vane.core.menu;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class MenuManager extends Listener<Core> {
	private final HashMap<UUID, Menu> open_menus = new HashMap<>();
	private final HashMap<Inventory, Menu> menus = new HashMap<>();
	//TODO private final HashMap<Inventory, List<UUID>> menu_viewers = new HashMap<>();

	public MenuManager(Context<Core> context) {
		super(context);
	}

	public Menu menu_for(final Player player, final InventoryView view) {
		return menu_for(player, view.getTopInventory());
	}

	public Menu menu_for(final Player player, final Inventory inventory) {
		final var menu = menus.get(inventory);
		if (open_menus.get(player.getUniqueId()) != menu) {
			get_module().log.warning("Menu inconsistency: entity " + player + " accessed a menu that isn't registered to it");
			return null;
		}
		return menu;
	}

	public void add(final Player player, final Menu menu) {
		open_menus.put(player.getUniqueId(), menu);
		menus.put(menu.inventory(), menu);
		//TODO menu_viewers.put(menu.inventory(), player.getUniqueId());
	}

	public void remove(final Player player, final Menu menu) {
		open_menus.remove(player.getUniqueId());
		final var orphaned = open_menus.values().stream()
			.allMatch(m -> m != menu);

		// Remove orphaned menus from other maps
		if (orphaned) {
			menus.remove(menu.inventory());
			//TODO menu_viewers.remove(menu.inventory());
		}
	}

	public void update(final Menu menu) {
		get_module().getServer().getOnlinePlayers().stream()
			.filter(p -> open_menus.get(p.getUniqueId()) == menu)
			.forEach(p -> p.updateInventory());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_inventory_click(final InventoryClickEvent event) {
		final var clicker = event.getWhoClicked();
		if (!(clicker instanceof Player)) {
			return;
		}

		final var player = (Player)clicker;
		final var menu = menu_for(player, event.getView());
		if (menu != null) {
			event.setCancelled(true);
			menu.click(player, event.getCurrentItem(), event.getRawSlot(), event.getClick(), event.getAction());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_inventory_drag(final InventoryDragEvent event) {
		final var clicker = event.getWhoClicked();
		if (!(clicker instanceof Player)) {
			return;
		}

		final var player = (Player)clicker;
		final var menu = menu_for(player, event.getView());
		if (menu != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void on_inventory_close(final InventoryCloseEvent event) {
		final var closer = event.getPlayer();
		if (!(closer instanceof Player)) {
			return;
		}

		final var player = (Player)closer;
		final var menu = menu_for(player, event.getView());
		if (menu != null) {
			menu.closed(player);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_anvil_event(final PrepareAnvilEvent event) {
		final var menu = menu_for((Player)event.getView().getPlayer(), event.getView());
		if (menu != null) {
			event.getInventory().setRepairCost(0);
			// TODO updateInventory
		}
	}
}
