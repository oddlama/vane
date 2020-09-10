package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.ItemUtil.name_of;
import static org.oddlama.vane.util.ItemUtil.name_item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class MenuFactory {
	public static Menu anvil_string_input(final Context<?> context, final Player player, final String title, final ItemStack input_item, final Function3<Player, Menu, String, ClickResult> on_click) {
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

		for (int i = 0; i < columns; ++i) {
			if (i == confirm_index) {
				confirmation_menu.add(new MenuItem(i, item_confirm, (player, menu, self) -> {
					menu.close(player);
					on_confirm.apply(player);
					return ClickResult.SUCCESS;
				}));
			} else {
				confirmation_menu.add(new MenuItem(i, item_cancel, (player, menu, self) -> {
					menu.close(player);
					if (menu.get_on_close() != on_cancel) {
						on_cancel.apply(player);
					}
					return ClickResult.SUCCESS;
				}));
			}
		}

		// On close call cancel
		confirmation_menu.on_close(on_cancel);

		return confirmation_menu;
	}

	public static Menu item_selector(final Context<?> context, final Player player, final String title, @Nullable final ItemStack initial_item, boolean allow_nothing, final Consumer2<Player, ItemStack> on_confirm, final Consumer1<Player> on_cancel) {
		return item_selector(context, player, title, initial_item, allow_nothing, on_confirm, on_cancel, i -> i);
	}

	public static Menu item_selector(final Context<?> context, final Player player, final String title, @Nullable final ItemStack initial_item, boolean allow_nothing, final Consumer2<Player, ItemStack> on_confirm, final Consumer1<Player> on_cancel, final Function1<ItemStack, ItemStack> on_select_item) {
		final var menu_manager = context.get_module().core.menu_manager;
		final Function1<ItemStack, ItemStack> set_item_name = (item) -> {
			return name_item(item, menu_manager.item_selector_selected.lang_name.format(), menu_manager.item_selector_selected.lang_lore.format());
		};

		final var no_item = set_item_name.apply(new ItemStack(Material.BARRIER));
		final ItemStack default_item;
		if (initial_item == null) {
			default_item = no_item;
		} else {
			default_item = initial_item;
		}

		final var columns = 9;
		final var item_selector_menu = new Menu(context, Bukkit.createInventory(null, columns, title));
		final var selected_item = new MenuItem(4, default_item, (p, menu, self, type, action) -> {
			if (!Menu.is_left_or_right_click(type, action)) {
				return ClickResult.INVALID_CLICK;
			}

			if (allow_nothing && type == ClickType.RIGHT) {
				// Clear selection
				self.item(no_item);
			} else {
				// Reset selection
				self.item(default_item);
			}
			menu.update();
			return ClickResult.SUCCESS;
		}) {
			public ItemStack original_selected = null;

			@Override
			public void item(final ItemStack item) {
				this.original_selected = item;
				super.item(set_item_name.apply(item.clone()));
			}
		};
		selected_item.original_selected = default_item;

		// Selected item
		item_selector_menu.add(selected_item);

		// Inventory listener
		item_selector_menu.add(new MenuItemClickListener(-1, (p, menu, item) -> {
			// Called when any item in inventory is clicked
			if (item == null) {
				return ClickResult.IGNORE;
			}

			// Call on_select and check if the resulting item is valid
			item = on_select_item.apply(item);
			if (item == null) {
				return ClickResult.ERROR;
			}

			// TODO serializable to bytes
			selected_item.item(item.clone());
			menu.update();
			return ClickResult.SUCCESS;
		}));

		// Accept item
		item_selector_menu.add(new MenuItem(2, menu_manager.item_selector_accept.item(), (p, menu, self) -> {
			final ItemStack item;
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
			on_confirm.apply(p, item);
			return ClickResult.SUCCESS;
		}));

		// Cancel item
		item_selector_menu.add(new MenuItem(6, menu_manager.item_selector_cancel.item(), (p, menu, self) -> {
			menu.close(p);
			if (menu.get_on_close() != on_cancel) {
				on_cancel.apply(player);
			}
			return ClickResult.SUCCESS;
		}));

		// On close call cancel
		item_selector_menu.on_close(on_cancel);

		return item_selector_menu;
	}
}
