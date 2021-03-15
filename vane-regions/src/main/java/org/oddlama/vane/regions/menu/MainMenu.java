package org.oddlama.vane.regions.menu;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.regions.region.RoleSetting;

public class MainMenu extends ModuleComponent<Regions> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_select_region_title;
	@LangMessage public TranslatedMessage lang_filter_regions_title;

	public TranslatedItemStack<?> item_create_region_start_selection;
	public TranslatedItemStack<?> item_create_region_invalid_selection;
	public TranslatedItemStack<?> item_create_region_valid_selection;
	public TranslatedItemStack<?> item_cancel_selection;
	public TranslatedItemStack<?> item_current_region;
	public TranslatedItemStack<?> item_list_regions;
	public TranslatedItemStack<?> item_select_region;
	public TranslatedItemStack<?> item_create_region_group;
	public TranslatedItemStack<?> item_current_region_group;
	public TranslatedItemStack<?> item_list_region_groups;
	public TranslatedItemStack<?> item_select_region_group;

	public MainMenu(Context<Regions> context) {
		super(context.namespace("main"));

		final var ctx = get_context();
        item_create_region_start_selection   = new TranslatedItemStack<>(ctx, "create_region_start_selection",   Material.WRITABLE_BOOK,        1, "Used to start creating a new region selection.");
        item_create_region_invalid_selection = new TranslatedItemStack<>(ctx, "create_region_invalid_selection", Material.BARRIER,              1, "Used to indicate an invalid selection.");
        item_create_region_valid_selection   = new TranslatedItemStack<>(ctx, "create_region_valid_selection",   Material.WRITABLE_BOOK,        1, "Used to create a new region with the current selection.");
        item_cancel_selection                = new TranslatedItemStack<>(ctx, "cancel_selection",                Material.RED_TERRACOTTA,       1, "Used to cancel region selection.");
        item_list_regions                    = new TranslatedItemStack<>(ctx, "list_regions",                    Material.COMPASS,              1, "Used to select a region the player may administrate.");
        item_select_region                   = new TranslatedItemStack<>(ctx, "select_region",                   Material.FILLED_MAP,           1, "Used to represent a region in the region selection list.");
        item_current_region                  = new TranslatedItemStack<>(ctx, "current_region",                  Material.FILLED_MAP,           1, "Used to access the region the player currently stands in.");
        item_create_region_group             = new TranslatedItemStack<>(ctx, "create_region_group",             Material.WRITABLE_BOOK,        1, "Used to create a new region group.");
        item_list_region_groups              = new TranslatedItemStack<>(ctx, "list_region_groups",              Material.COMPASS,              1, "Used to select a region group the player may administrate.");
        item_current_region_group            = new TranslatedItemStack<>(ctx, "current_region_group",            Material.GLOBE_BANNER_PATTERN, 1, "Used to access the region group associated with the region the player currently stands in.");
        item_select_region_group             = new TranslatedItemStack<>(ctx, "select_region_group",             Material.GLOBE_BANNER_PATTERN, 1, "Used to represent a region group in the region group selection list.");
	}

	public Menu create(final Player player) {
		final var columns = 9;
		final var title = lang_title.str();
		final var main_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));

		final var selection_mode = get_module().is_selecting_region(player);
		final var region = get_module().region_at(player.getLocation());
		if (region != null) {
			main_menu.tag(new RegionMenuTag(region.id()));
		}

		// Check if target selection would be allowed
		if (selection_mode) {
			final var selection = get_module().get_region_selection(player);
			main_menu.add(menu_item_create_region(selection));
			main_menu.add(menu_item_cancel_selection());
		} else {
			main_menu.add(menu_item_start_selection());
			main_menu.add(menu_item_list_regions());
			if (region != null) {
				main_menu.add(menu_item_current_region(region));
			}
		}

		main_menu.add(menu_item_create_region_group());
		main_menu.add(menu_item_list_region_groups());
		if (region != null) {
			main_menu.add(menu_item_current_region_group(region.region_group(get_module())));
		}

		return main_menu;
	}

	private MenuWidget menu_item_start_selection() {
		return new MenuItem(0, item_create_region_start_selection.item(), (player, menu, self) -> {
			menu.close(player);
			get_module().start_region_selection(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_cancel_selection() {
		return new MenuItem(1, item_cancel_selection.item(), (player, menu, self) -> {
			menu.close(player);
			get_module().cancel_region_selection(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_create_region(final RegionSelection selection) {
		return new MenuItem(0, null, (player, menu, self) -> {
			if (selection.is_valid()) {
				menu.close(player);

				get_module().menus.enter_region_name_menu.create(player, (player2, name) -> {
					return ClickResult.SUCCESS;
				}).on_natural_close(player2 -> {
					menu.open(player2);
				}).open(player);

				return ClickResult.SUCCESS;
			} else {
				return ClickResult.ERROR;
			}
		}) {
			@Override
			public void item(final ItemStack item) {
				if (selection.is_valid()) {
					final var dx = 1 + Math.abs(selection.primary.getX() - selection.secondary.getX());
					final var dy = 1 + Math.abs(selection.primary.getY() - selection.secondary.getY());
					final var dz = 1 + Math.abs(selection.primary.getZ() - selection.secondary.getZ());
					super.item(item_create_region_valid_selection.item(
							"§a" + dx,
							"§a" + dy,
							"§a" + dz
						));
				} else {
					boolean is_primary_set = selection.primary != null;
					boolean is_secondary_set = selection.secondary != null;
					boolean same_world = selection.primary.getWorld().equals(selection.secondary.getWorld());
					is_secondary_set &= same_world;

					boolean minimum_satisified, maximum_satisfied;
					String sdx, sdy, sdz;
					if (is_primary_set && is_secondary_set) {
						final var dx = 1 + Math.abs(selection.primary.getX() - selection.secondary.getX());
						final var dy = 1 + Math.abs(selection.primary.getY() - selection.secondary.getY());
						final var dz = 1 + Math.abs(selection.primary.getZ() - selection.secondary.getZ());
						sdx = Integer.toString(dx);
						sdy = Integer.toString(dy);
						sdz = Integer.toString(dz);

						minimum_satisified =
							dx >= get_module().config_min_region_extent_x &&
							dy >= get_module().config_min_region_extent_y &&
							dz >= get_module().config_min_region_extent_z;
						maximum_satisfied =
							dx <= get_module().config_max_region_extent_x &&
							dy <= get_module().config_max_region_extent_y &&
							dz <= get_module().config_max_region_extent_z;
					} else {
						sdx = "§7?";
						sdy = "§7?";
						sdz = "§7?";
						minimum_satisified = false;
						maximum_satisfied = false;
					}

					final var extent_color = minimum_satisified && maximum_satisfied ? "§a" : "§c";
					final var no_intersection = !selection.intersects_existing();

					super.item(item_create_region_invalid_selection.item(
							is_primary_set     ? "§a✓" : "§c✗✕",
							is_secondary_set   ? "§a✓" : "§c✗✕",
							no_intersection    ? "§a✓" : "§c✗✕",
							minimum_satisified ? "§a✓" : "§c✗✕",
							maximum_satisfied  ? "§a✓" : "§c✗✕",
							extent_color + sdx,
							extent_color + sdy,
							extent_color + sdz,
							"§b" + get_module().config_min_region_extent_x,
							"§b" + get_module().config_min_region_extent_y,
							"§b" + get_module().config_min_region_extent_z,
							"§b" + get_module().config_max_region_extent_x,
							"§b" + get_module().config_max_region_extent_y,
							"§b" + get_module().config_max_region_extent_z
						));
				}
			}
		};
	}

	private MenuWidget menu_item_list_regions() {
		return new MenuItem(1, item_list_regions.item(), (player, menu, self) -> {
			menu.close(player);
			final var all_regions = get_module().all_regions()
				.stream()
				.filter(r -> r.region_group(get_module())
				             .get_role(player.getUniqueId())
				             .get_setting(RoleSetting.ADMIN))
				.sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
				.collect(Collectors.toList());

			final var filter = new Filter.StringFilter<Region>((r, str) -> r.name().toLowerCase().contains(str));
			MenuFactory.generic_selector(get_context(), player, lang_select_region_title.str(), lang_filter_regions_title.str(), all_regions,
				r -> item_select_region.item("§a§l" + r.name()),
				filter,
				(player2, m, region) -> {
					m.close(player2);
					// TODO get_module().menus.region_menu.create(player2, region).open(player2);
					return ClickResult.SUCCESS;
				}, player2 -> {
					menu.open(player2);
				}).open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_current_region(final Region region) {
		return new MenuItem(2, item_current_region.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_create_region_group() {
		return new MenuItem(6, item_create_region_group.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_list_region_groups() {
		return new MenuItem(7, item_list_region_groups.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_current_region_group(final RegionGroup region_group) {
		return new MenuItem(8, item_current_region_group.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
