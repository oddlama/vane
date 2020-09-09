package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.ItemUtil.translate_item;
import static org.oddlama.vane.util.ItemUtil.name_of;

import net.md_5.bungee.api.chat.BaseComponent;
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
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function1;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.module.Context;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuFactory {
	public static Menu anvil_string_input_menu(final Context<?> context, final Player player, final String title, final ItemStack input_item, final Function3<Player, Menu, String, ClickResult> on_click) {
		final var anvil = new AnvilMenu(context, player, title);
		anvil.add(new MenuItem(0, input_item));
		anvil.add(new MenuItemClickListener(2, (p, menu, item) -> {
			return on_click.apply(p, menu, name_of(item));
		}));
		return anvil;
	}

	public static Menu confirm(final Context<?> context, final String title, final ItemStack item_confirm, final Consumer1<Player> on_confirm, final ItemStack item_cancel, final Consumer1<Player> on_cancel) {
		final var columns = 9;
		final var confirmation_menu = new Menu(context, Bukkit.createInventory(null, columns, title));
		final var confirm_index = (int)(Math.random() * columns);

		for (int i = 0; i < 9; ++i) {
			if (i == confirm_index) {
				confirmation_menu.add(new MenuItem(i, item_confirm, (player, menu, self) -> {
					menu.close(player);
					on_confirm.apply(player);
					return ClickResult.SUCCESS;
				}));
			} else {
				confirmation_menu.add(new MenuItem(i, item_cancel, (player, menu, self) -> {
					menu.close(player);
					on_cancel.apply(player);
					return ClickResult.SUCCESS;
				}));
			}
		}

		return confirmation_menu;
	}

	public static Menu item_chooser(final Context<?> context, final Player player, final String title, @Nullable final ItemStack initial_item, boolean allow_nothing, final Consumer2<Player, ItemStack> on_confirm, final Function2<Player, Menu, ClickResult> on_cancel, final Function1<ItemStack, ItemStack> on_select_item) {
		final Function1<ItemStack, ItemStack> set_item_name = (item) -> {
			return translate_item(item, "", "");
		};

		final var no_item = set_item_name.apply(new ItemStack(Material.BARRIER));
		if (initial_item == null) {
			initial_item = no_item;
		}

		final var columns = 9;
		final var item_chooser_menu = new Menu(context, Bukkit.createInventory(null, columns, title));
		final var selected_item = new MenuItem(4, initial_item, (p, menu, self, type, action) -> {
			if (!Menu.is_normal_click(type, action)) {
				return ClickResult.INVALID_CLICK;
			}

			if (allow_nothing && type == ClickType.RIGHT) {
				// Clear selection
				selected_item.item(no_item);
			} else {
				// Reset selection
				selected_item.item(initial_item);
			}
			menu.update();
		}) {
			public ItemStack original_selected = null;
			@Override
			public void item(final ItemStack item) {
				this.original_selected = item;
				super.item(set_item_name(item));
			}
		};
		selected_item.original_selected = initial_item;

		item_chooser_menu.add(selected_item);
		item_chooser_menu.add(new MenuItemClickListener(-1, (p, menu, item) -> {
			// Called when any item in inventory is clicked
			if (item == null) {
				return ClickResult.IGNORE;
			}

			// Call on_select and check if the resulting item is valid
			item = on_select_item(item);
			if (item == null) {
				return ClickResult.ERROR;
			}

			// TODO serializable to bytes
			selected_item.item(item.clone());
			menu.update();
			return ClickResult.SUCCESS;
		}));

		item_chooser_menu.add(new MenuItem(2, menu.manager().item_chooser_accept.clone(), (p, menu, self) -> {
			final var item;
			if (selected_item.original_selected == no_item) {
				if (allow_nothing) {
					item = null;
				} else {
					return ClickResult.ERROR;
				}
			} else {
				item = selected_item.original_selected;
			}

			menu.close(p);
			on_confirm.accept(item);
			return ClickResult.SUCCESS;
		}));

		item_chooser_menu.add(new MenuItem(6, menu.manager().item_chooser_cancel, (p, menu, self) -> {
			menu.close(p);
			on_cancel.accept(p);
		}));

		return item_chooser_menu;
	}
}
