package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.ItemUtil.name_of;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.material.HeadMaterial;
import org.oddlama.vane.core.material.HeadMaterialLibrary;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class MenuFactory {

	public static Menu anvil_string_input(
		final Context<?> context,
		final Player player,
		final String title,
		final ItemStack _input_item,
		final String default_name,
		final Function3<Player, Menu, String, ClickResult> on_click
	) {
		final var input_item = _input_item.clone();
		final var meta = input_item.getItemMeta();
		meta.displayName(LegacyComponentSerializer.legacySection().deserialize(default_name));
		input_item.setItemMeta(meta);

		final var anvil = new AnvilMenu(context, player, title);
		anvil.add(new MenuItem(0, input_item));
		anvil.add(
			new MenuItemClickListener(
				2,
				(p, menu, item) -> on_click.apply(p, menu, name_of(item))
			)
		);
		return anvil;
	}

	public static Menu confirm(
		final Context<?> context,
		final String title,
		final ItemStack item_confirm,
		final Function1<Player, ClickResult> on_confirm,
		final ItemStack item_cancel,
		final Consumer1<Player> on_cancel
	) {
		final var columns = 9;
		final var confirmation_menu = new Menu(
			context,
			Bukkit.createInventory(null, columns, LegacyComponentSerializer.legacySection().deserialize(title))
		);
		final var confirm_index = (int) (Math.random() * columns);

		for (int i = 0; i < columns; ++i) {
			if (i == confirm_index) {
				confirmation_menu.add(
					new MenuItem(
						i,
						item_confirm,
						(player, menu, self) -> {
							menu.close(player);
							return on_confirm.apply(player);
						}
					)
				);
			} else {
				confirmation_menu.add(
					new MenuItem(
						i,
						item_cancel,
						(player, menu, self) -> {
							menu.close(player);
							on_cancel.apply(player);
							return ClickResult.SUCCESS;
						}
					)
				);
			}
		}

		// On natural close call cancel
		confirmation_menu.on_natural_close(on_cancel);

		return confirmation_menu;
	}

	public static Menu item_selector(
		final Context<?> context,
		final Player player,
		final String title,
		@Nullable final ItemStack initial_item,
		boolean allow_nothing,
		final Function2<Player, ItemStack, ClickResult> on_confirm,
		final Consumer1<Player> on_cancel
	) {
		return item_selector(context, player, title, initial_item, allow_nothing, on_confirm, on_cancel, i -> i);
	}

	public static Menu item_selector(
		final Context<?> context,
		final Player player,
		final String title,
		@Nullable final ItemStack initial_item,
		boolean allow_nothing,
		final Function2<Player, ItemStack, ClickResult> on_confirm,
		final Consumer1<Player> on_cancel,
		final Function1<ItemStack, ItemStack> on_select_item
	) {
		final var menu_manager = context.get_module().core.menu_manager;
		final Function1<ItemStack, ItemStack> set_item_name = item -> name_item(
			item,
			menu_manager.item_selector_selected.lang_name.format(),
			menu_manager.item_selector_selected.lang_lore.format()
		);

		final var no_item = set_item_name.apply(new ItemStack(Material.BARRIER));
		final ItemStack default_item;
		if (initial_item == null || initial_item.getType() == Material.AIR) {
			default_item = no_item;
		} else {
			default_item = initial_item;
		}

		final var columns = 9;
		final var item_selector_menu = new Menu(
			context,
			Bukkit.createInventory(null, columns, LegacyComponentSerializer.legacySection().deserialize(title))
		);
		final var selected_item = new MenuItem(
			4,
			default_item,
			(p, menu, self, event) -> {
				if (!Menu.is_left_or_right_click(event)) {
					return ClickResult.INVALID_CLICK;
				}

				if (allow_nothing && event.getClick() == ClickType.RIGHT) {
					// Clear selection
					self.update_item(menu, no_item);
				} else {
					// Reset selection
					self.update_item(menu, default_item);
				}
				return ClickResult.SUCCESS;
			}
		) {
			public ItemStack original_selected = null;

			@Override
			public void item(final ItemStack item) {
				this.original_selected = item;
				super.item(set_item_name.apply(item.clone()));
			}
		};

		// Selected item, begin with default selected
		selected_item.item(default_item);
		item_selector_menu.add(selected_item);

		// Inventory listener
		item_selector_menu.add(
			new MenuItemClickListener(
				-1,
				(p, menu, item) -> {
					// Called when any item in inventory is clicked
					if (item == null) {
						return ClickResult.IGNORE;
					}

					// Call on_select and check if the resulting item is valid
					item = on_select_item.apply(item.clone());
					if (item == null) {
						return ClickResult.ERROR;
					}

					selected_item.item(item);
					menu.update();
					return ClickResult.SUCCESS;
				}
			)
		);

		// Accept item
		item_selector_menu.add(
			new MenuItem(
				2,
				menu_manager.item_selector_accept.item(),
				(p, menu, self) -> {
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
					return on_confirm.apply(p, item);
				}
			)
		);

		// Cancel item
		item_selector_menu.add(
			new MenuItem(
				6,
				menu_manager.item_selector_cancel.item(),
				(p, menu, self) -> {
					menu.close(p);
					on_cancel.apply(player);
					return ClickResult.SUCCESS;
				}
			)
		);

		// On natural close call cancel
		item_selector_menu.on_natural_close(on_cancel);

		return item_selector_menu;
	}

	public static <T, F extends Filter<T>> Menu generic_selector(
		final Context<?> context,
		final Player player,
		final String title,
		final String filter_title,
		final List<T> things,
		final Function1<T, ItemStack> to_item,
		final F filter,
		final Function3<Player, Menu, T, ClickResult> on_click,
		final Consumer1<Player> on_cancel
	) {
		return generic_selector(
			context,
			player,
			title,
			filter_title,
			things,
			to_item,
			filter,
			(p, menu, t, event) -> {
				if (!Menu.is_left_click(event)) {
					return ClickResult.INVALID_CLICK;
				}
				return on_click.apply(p, menu, t);
			},
			on_cancel
		);
	}

	public static <T, F extends Filter<T>> Menu generic_selector(
		final Context<?> context,
		final Player player,
		final String title,
		final String filter_title,
		final List<T> things,
		final Function1<T, ItemStack> to_item,
		final F filter,
		final Function4<Player, Menu, T, InventoryClickEvent, ClickResult> on_click,
		final Consumer1<Player> on_cancel
	) {
		return GenericSelector.create(
			context,
			player,
			title,
			filter_title,
			things,
			to_item,
			filter,
			on_click,
			on_cancel
		);
	}

	public static Menu head_selector(
		final Context<?> context,
		final Player player,
		final Function3<Player, Menu, HeadMaterial, ClickResult> on_click,
		final Consumer1<Player> on_cancel
	) {
		return head_selector(
			context,
			player,
			(p, menu, t, event) -> {
				if (!Menu.is_left_click(event)) {
					return ClickResult.INVALID_CLICK;
				}
				return on_click.apply(p, menu, t);
			},
			on_cancel
		);
	}

	public static Menu head_selector(
		final Context<?> context,
		final Player player,
		final Function4<Player, Menu, HeadMaterial, InventoryClickEvent, ClickResult> on_click,
		final Consumer1<Player> on_cancel
	) {
		final var menu_manager = context.get_module().core.menu_manager;
		final var all_heads = HeadMaterialLibrary
			.all()
			.stream()
			.sorted((a, b) -> a.key().toString().compareToIgnoreCase(b.key().toString()))
			.collect(Collectors.toList());

		final var filter = new HeadFilter();
		return MenuFactory.generic_selector(
			context,
			player,
			menu_manager.head_selector.lang_title.str("§5§l" + all_heads.size()),
			menu_manager.head_selector.lang_filter_title.str(),
			all_heads,
			h ->
				menu_manager.head_selector.item_select_head.alternative(
					h.item(),
					"§a§l" + h.name(),
					"§6" + h.category(),
					"§b" + h.tags()
				),
			filter,
			on_click,
			on_cancel
		);
	}
}
