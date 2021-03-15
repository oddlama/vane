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
import org.oddlama.vane.regions.region.Role;

public class RegionGroupMenu extends ModuleComponent<Regions> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_delete_confirm_title;
	@LangMessage public TranslatedMessage lang_select_role_title;
	@LangMessage public TranslatedMessage lang_filter_roles_title;

	public TranslatedItemStack<?> item_rename;
	public TranslatedItemStack<?> item_delete;
	public TranslatedItemStack<?> item_delete_confirm_accept;
	public TranslatedItemStack<?> item_delete_confirm_cancel;
	public TranslatedItemStack<?> item_environment_settings;
	public TranslatedItemStack<?> item_create_role;
	public TranslatedItemStack<?> item_list_roles;
	public TranslatedItemStack<?> item_select_role;

	public RegionGroupMenu(Context<Regions> context) {
		super(context.namespace("region_group"));

		final var ctx = get_context();
        item_rename                = new TranslatedItemStack<>(ctx, "rename",                Material.NAME_TAG,                          1, "Used to rename the region group.");
        item_delete                = new TranslatedItemStack<>(ctx, "delete",                namespaced_key("vane", "decoration_tnt_1"), 1, "Used to delete this region group.");
        item_delete_confirm_accept = new TranslatedItemStack<>(ctx, "delete_confirm_accept", namespaced_key("vane", "decoration_tnt_1"), 1, "Used to confirm deleting the region group.");
        item_delete_confirm_cancel = new TranslatedItemStack<>(ctx, "delete_confirm_cancel", Material.PRISMARINE_SHARD,                  1, "Used to cancel deleting the region group.");
		// TODO icon...
        item_environment_settings  = new TranslatedItemStack<>(ctx, "environment_settings",  Material.FLOWER_POT,                        1, "Used to open the environment settings.");
        item_create_role           = new TranslatedItemStack<>(ctx, "create_role",           Material.WRITABLE_BOOK,                     1, "Used to create a new role.");
        item_list_roles            = new TranslatedItemStack<>(ctx, "list_roles",            Material.GLOBE_BANNER_PATTERN,              1, "Used to list all defined roles.");
        item_select_role           = new TranslatedItemStack<>(ctx, "select_role",           Material.GLOBE_BANNER_PATTERN,              1, "Used to represent a role in the role selection list.");
	}

	public Menu create(final RegionGroup group, final Player player) {
		final var columns = 9;
		final var title = lang_title.str();
		final var region_group_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));
		region_group_menu.tag(new RegionGroupMenuTag(group.id()));

		final var is_owner = player.getUniqueId().equals(group.owner());
		if (is_owner) {
			region_group_menu.add(menu_item_rename(group));
			region_group_menu.add(menu_item_delete(group));
		}

		region_group_menu.add(menu_item_environment_settings(group));
		region_group_menu.add(menu_item_create_role(group));
		region_group_menu.add(menu_item_list_roles(group));

		return region_group_menu;
	}

	private MenuWidget menu_item_rename(final RegionGroup group) {
		return new MenuItem(0, item_rename.item(), (player, menu, self) -> {
			menu.close(player);

			get_module().menus.enter_region_group_name_menu.create(player, group.name(), (player2, name) -> {
				group.name(name);
				mark_persistent_storage_dirty();

				// Open new menu because of possibly changed title
				get_module().menus.region_group_menu.create(group, player2).open(player2);
				return ClickResult.SUCCESS;
			}).on_natural_close(player2 -> {
				// Open new menu because of possibly changed title
				get_module().menus.region_group_menu.create(group, player2).open(player2);
			}).open(player);

			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_delete(final RegionGroup group) {
		final var orphan_checkbox = group.is_orphan(get_module()) ? "§a✓" : "§c✕";
		return new MenuItem(1, item_delete.item(orphan_checkbox), (player, menu, self) -> {
			if (!group.is_orphan(get_module())) {
				return ClickResult.ERROR;
			}

			menu.close(player);
			MenuFactory.confirm(get_context(), lang_delete_confirm_title.str(),
				item_delete_confirm_accept.item(), (player2) -> {
					if (!player2.getUniqueId().equals(group.owner())) {
						return ClickResult.ERROR;
					}

					get_module().remove_region_group(group);
					return ClickResult.SUCCESS;
				}, item_delete_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
				.tag(new RegionMenuTag(region.id()))
				.open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_environment_settings(final RegionGroup group) {
		return new MenuItem(4, item_environment_settings.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_create_role(final RegionGroup group) {
		return new MenuItem(7, item_create_role.item(), (player, menu, self) -> {
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_list_roles(final RegionGroup group) {
		return new MenuItem(8, item_list_roles.item(), (player, menu, self) -> {
			menu.close(player);
			final var all_roles = group.roles()
				.stream()
				.sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
				.collect(Collectors.toList());

			final var filter = new Filter.StringFilter<Role>((r, str) -> r.name().toLowerCase().contains(str));
			MenuFactory.generic_selector(get_context(), player, lang_select_role_title.str(), lang_filter_roles_title.str(), all_roles,
				r -> item_select_role.item("§a§l" + r.name()),
				filter,
				(player2, m, role) -> {
					m.close(player2);
					// TODO get_module().menus.role_menu.create(player2, role).open(player2);
					return ClickResult.SUCCESS;
				}, player2 -> {
					menu.open(player2);
				}).open(player);
			return ClickResult.SUCCESS;
		});
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
