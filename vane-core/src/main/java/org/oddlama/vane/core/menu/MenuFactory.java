package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.ItemUtil.name_of;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.module.Context;

public class MenuFactory {
	public static Menu anvil_string_input_menu(Context<?> context, final Player player, final String title, final ItemStack input_item, final Function3<Player, Menu, String, ClickResult> on_click) {
		final var anvil = new AnvilMenu(context, player, title);
		anvil.add(new MenuItem(0, input_item));
		anvil.add(new MenuItemClickListener(2, (p, menu, item, type, action) -> {
			if (!Menu.is_normal_click(type, action)) {
				return ClickResult.INVALID_CLICK;
			}
			return on_click.apply(p, menu, name_of(item));
		}));
		return anvil;
	}
}
