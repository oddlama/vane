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

public class MainMenu extends ModuleComponent<Regions> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_unlink_console_confirm_title;
	@LangMessage public TranslatedMessage lang_destroy_region_confirm_title;
	@LangMessage public TranslatedMessage lang_select_target_title;
	@LangMessage public TranslatedMessage lang_filter_regions_title;

	public TranslatedItemStack<?> item_create_region_start_selection;
	public TranslatedItemStack<?> item_create_region_invalid_selection;
	public TranslatedItemStack<?> item_create_region_valid_selection;
	public TranslatedItemStack<?> item_cancel_selection;
	public TranslatedItemStack<?> item_current_region;
	public TranslatedItemStack<?> item_list_regions;
	public TranslatedItemStack<?> item_create_region_group;
	public TranslatedItemStack<?> item_current_region_group;
	public TranslatedItemStack<?> item_list_region_groups;

	public MainMenu(Context<Regions> context) {
		super(context.namespace("main"));

		final var ctx = get_context();
        item_create_region_start_selection   = new TranslatedItemStack<>(ctx, "create_region_start_selection",   Material.WRITABLE_BOOK,        1, "Used to start creating a new region selection.");
        item_create_region_invalid_selection = new TranslatedItemStack<>(ctx, "create_region_invalid_selection", Material.BARRIER,              1, "Used to indicate an invalid selection.");
        item_create_region_valid_selection   = new TranslatedItemStack<>(ctx, "create_region_valid_selection",   Material.WRITABLE_BOOK,        1, "Used to create a new region with the current selection.");
        item_cancel_selection                = new TranslatedItemStack<>(ctx, "cancel_selection",                Material.RED_TERRACOTTA,       1, "Used to cancel region selection.");
        item_list_regions                    = new TranslatedItemStack<>(ctx, "list_regions",                    Material.COMPASS,              1, "Used to select a region the player may administrate.");
        item_current_region                  = new TranslatedItemStack<>(ctx, "current_region",                  Material.FILLED_MAP,           1, "Used to access the region the player currently stands in.");
        item_create_region_group             = new TranslatedItemStack<>(ctx, "create_region_group",             Material.WRITABLE_BOOK,        1, "Used to create a new region group.");
        item_list_region_groups              = new TranslatedItemStack<>(ctx, "list_region_groups",              Material.COMPASS,              1, "Used to select a region group the player may administrate.");
        item_current_region_group            = new TranslatedItemStack<>(ctx, "current_region_group",            Material.GLOBE_BANNER_PATTERN, 1, "Used to access the region group associated with the region the player currently stands in.");
	}

	public Menu create(final Player player) {
		final var columns = 9;
		final var title = lang_title.str();
		final var main_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));

		final var selection_mode = false; //TODO
		final var region = get_module().region_at(player.getLocation());

		// Check if target selection would be allowed
		if (selection_mode) {
			main_menu.add(menu_item_create_region());
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
			main_menu.add(menu_item_current_region_group(region.group(get_module())));
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

	private MenuWidget menu_item_select_target(final Region region) {
		return new MenuItem(4, null, (player, menu, self) -> {
			if (region.target_locked()) {
				return ClickResult.ERROR;
			} else {
				menu.close(player);
				final var all_regions = get_module().all_regions()
					.stream()
					.filter(p -> {
						switch (p.visibility()) {
							case PUBLIC:  return true;
							case GROUP:   return false; // TODO group visibility
							case PRIVATE: return player.getUniqueId().equals(p.owner());
						}
						return false;
					})
					.filter(p -> !Objects.equals(p.id(), region.id()))
					.sorted(new Region.TargetSelectionComparator(player))
					.collect(Collectors.toList());

				final var filter = new Filter.StringFilter<Region>((p, str) -> p.name().toLowerCase().contains(str));
				MenuFactory.generic_selector(get_context(), player, lang_select_target_title.str(), lang_filter_regions_title.str(), all_regions,
					p -> {
						final var dist = p.spawn().toVector().setY(0.0).distance(player.getLocation().toVector().setY(0.0));
						return item_select_target_region.alternative(get_module().icon_for(p), "§a§l" + p.name(), "§6" + String.format("%.1f", dist), "§b" + p.spawn().getWorld().getName());
					},
					filter,
					(player2, m, t) -> {
						m.close(player2);

						final var select_target_event = new RegionSelectTargetEvent(player, region, t, false);
						get_module().getServer().getPluginManager().callEvent(select_target_event);
						if (select_target_event.isCancelled()) {
							get_module().lang_select_target_restricted.send(player2);
							return ClickResult.ERROR;
						}

						region.target_id(t.id());

						// Update region block to reflect new target on consoles
						region.update_blocks(get_module());
						mark_persistent_storage_dirty();
						return ClickResult.SUCCESS;
					}, player2 -> {
						menu.open(player2);
					}).tag(new RegionMenuTag(region.id())).open(player);
				return ClickResult.SUCCESS;
			}
		}) {
			@Override
			public void item(final ItemStack item) {
				final var target = region.target(get_module());
				final var target_name = "§a" + (target == null ? "None" : target.name());
				if (region.target_locked()) {
					super.item(item_select_target_locked.item(target_name));
				} else {
					super.item(item_select_target.item(target_name));
				}
			}
		};
	}

	private MenuWidget menu_item_unlink_console(final Region region, final Block console) {
		return new MenuItem(7, item_unlink_console.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.confirm(get_context(), lang_unlink_console_confirm_title.str(),
				item_unlink_console_confirm_accept.item(), (player2) -> {
					// Call event
					final var event = new RegionUnlinkConsoleEvent(player2, region, false);
					get_module().getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						get_module().lang_unlink_restricted.send(player2);
						return ClickResult.ERROR;
					}

					final var region_block = region.region_block_for(console);
					if (region_block == null) {
						// Console was likely already removed by another player
						return ClickResult.ERROR;
					}

					get_module().remove_region_block(region, region_block);
					return ClickResult.SUCCESS;
				}, item_unlink_console_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
				.tag(new RegionMenuTag(region.id()))
				.open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_destroy_region(final Region region) {
		return new MenuItem(8, item_destroy_region.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.confirm(get_context(), lang_destroy_region_confirm_title.str(),
				item_destroy_region_confirm_accept.item(), (player2) -> {
					// Call event
					final var event = new RegionDestroyEvent(player2, region, false);
					get_module().getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						get_module().lang_destroy_restricted.send(player2);
						return ClickResult.ERROR;
					}

					get_module().remove_region(region);
					return ClickResult.SUCCESS;
				}, item_destroy_region_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
				.tag(new RegionMenuTag(region.id()))
				.open(player);
			return ClickResult.SUCCESS;
		});
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
