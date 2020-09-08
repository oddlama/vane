package org.oddlama.vane.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.functional.Function5;

public class MenuItem implements MenuWidget {
	private int slot;
	private ItemStack item;
	private Function5<Player, Menu, MenuItem, ClickType, InventoryAction, ClickResult> on_click;

	public MenuItem(int slot, final ItemStack item) { this(slot, item, null); }
	public MenuItem(int slot, final ItemStack item, final Function5<Player, Menu, MenuItem, ClickType, InventoryAction, ClickResult> on_click) {
		this.slot = slot;
		this.item = item;
		this.on_click = on_click;
	}

	public void update(final Menu menu) {
		menu.inventory().setItem(slot, item);
	}

	public ClickResult click(final Player player, final Menu menu, final ItemStack item, int slot, final ClickType type, final InventoryAction action) {
		if (this.slot != slot) {
			return ClickResult.IGNORE;
		}

		if (on_click != null) {
			return on_click.apply(player, menu, this, type, action);
		} else {
			return ClickResult.IGNORE;
		}
	}
}
