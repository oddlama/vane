package org.oddlama.vane.regions.menu;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.EnvironmentSetting;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.text.Component;

public class RegionGroupMenu extends ModuleComponent<Regions> {

	@LangMessage
	public TranslatedMessage lang_title;

	@LangMessage
	public TranslatedMessage lang_delete_confirm_title;

	@LangMessage
	public TranslatedMessage lang_select_role_title;

	@LangMessage
	public TranslatedMessage lang_filter_roles_title;

	public TranslatedItemStack<?> item_rename;
	public TranslatedItemStack<?> item_delete;
	public TranslatedItemStack<?> item_delete_confirm_accept;
	public TranslatedItemStack<?> item_delete_confirm_cancel;
	public TranslatedItemStack<?> item_create_role;
	public TranslatedItemStack<?> item_list_roles;
	public TranslatedItemStack<?> item_select_role;

	public TranslatedItemStack<?> item_setting_toggle_on;
	public TranslatedItemStack<?> item_setting_toggle_off;
	public TranslatedItemStack<?> item_setting_info_animals;
	public TranslatedItemStack<?> item_setting_info_monsters;
	public TranslatedItemStack<?> item_setting_info_explosions;
	public TranslatedItemStack<?> item_setting_info_fire;
	public TranslatedItemStack<?> item_setting_info_pvp;
	public TranslatedItemStack<?> item_setting_info_trample;
	public TranslatedItemStack<?> item_setting_info_vine_growth;

	public RegionGroupMenu(Context<Regions> context) {
		super(context.namespace("region_group"));
		final var ctx = get_context();
		item_rename =
			new TranslatedItemStack<>(ctx, "rename", Material.NAME_TAG, 1, "Used to rename the region group.");
		item_delete =
			new TranslatedItemStack<>(
				ctx,
				"delete",
				StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
				1,
				"Used to delete this region group."
			);
		item_delete_confirm_accept =
			new TranslatedItemStack<>(
				ctx,
				"delete_confirm_accept",
				StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
				1,
				"Used to confirm deleting the region group."
			);
		item_delete_confirm_cancel =
			new TranslatedItemStack<>(
				ctx,
				"delete_confirm_cancel",
				Material.PRISMARINE_SHARD,
				1,
				"Used to cancel deleting the region group."
			);
		item_create_role =
			new TranslatedItemStack<>(ctx, "create_role", Material.WRITABLE_BOOK, 1, "Used to create a new role.");
		item_list_roles =
			new TranslatedItemStack<>(
				ctx,
				"list_roles",
				Material.GLOBE_BANNER_PATTERN,
				1,
				"Used to list all defined roles."
			);
		item_select_role =
			new TranslatedItemStack<>(
				ctx,
				"select_role",
				Material.GLOBE_BANNER_PATTERN,
				1,
				"Used to represent a role in the role selection list."
			);

		item_setting_toggle_on =
			new TranslatedItemStack<>(
				ctx,
				"setting_toggle_on",
				Material.GREEN_TERRACOTTA,
				1,
				"Used to represent a toggle button with current state on."
			);
		item_setting_toggle_off =
			new TranslatedItemStack<>(
				ctx,
				"setting_toggle_off",
				Material.RED_TERRACOTTA,
				1,
				"Used to represent a toggle button with current state off."
			);
		item_setting_info_animals =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_animals",
				StorageUtil.namespaced_key("vane", "animals_baby_pig_2"),
				1,
				"Used to represent the info for the animals setting."
			);
		item_setting_info_monsters =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_monsters",
				Material.ZOMBIE_HEAD,
				1,
				"Used to represent the info for the monsters setting."
			);
		item_setting_info_explosions =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_explosions",
				StorageUtil.namespaced_key("vane", "monsters_creeper_with_tnt_2"),
				1,
				"Used to represent the info for the explosions setting."
			);
		item_setting_info_fire =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_fire",
				Material.CAMPFIRE,
				1,
				"Used to represent the info for the fire setting."
			);
		item_setting_info_pvp =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_pvp",
				Material.IRON_SWORD,
				1,
				"Used to represent the info for the pvp setting."
			);
		item_setting_info_trample =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_trample",
				Material.FARMLAND,
				1,
				"Used to represent the info for the trample setting."
			);
		item_setting_info_vine_growth =
			new TranslatedItemStack<>(
				ctx,
				"setting_info_vine_growth",
				Material.VINE,
				1,
				"Used to represent the info for the vine growth setting."
			);
	}

	public Menu create(final RegionGroup group, final Player player) {
		final var columns = 9;
		final var rows = 3;
		final var title = lang_title.str_component("§5§l" + group.name());
		final var region_group_menu = new Menu(get_context(), Bukkit.createInventory(null, rows * columns, title));
		region_group_menu.tag(new RegionGroupMenuTag(group.id()));

		final var is_owner = player.getUniqueId().equals(group.owner());
		if (is_owner) {
			region_group_menu.add(menu_item_rename(group));
			// Delete only if this isn't the default group
			if (!get_module().get_or_create_default_region_group(player).id().equals(group.id())) {
				region_group_menu.add(menu_item_delete(group));
			}
		}

		region_group_menu.add(menu_item_create_role(group));
		region_group_menu.add(menu_item_list_roles(group));

		add_menu_item_setting(region_group_menu, group, 0, item_setting_info_animals, EnvironmentSetting.ANIMALS);
		add_menu_item_setting(region_group_menu, group, 1, item_setting_info_monsters, EnvironmentSetting.MONSTERS);
		add_menu_item_setting(region_group_menu, group, 3, item_setting_info_explosions, EnvironmentSetting.EXPLOSIONS);
		add_menu_item_setting(region_group_menu, group, 4, item_setting_info_fire, EnvironmentSetting.FIRE);
		add_menu_item_setting(region_group_menu, group, 5, item_setting_info_pvp, EnvironmentSetting.PVP);
		add_menu_item_setting(region_group_menu, group, 7, item_setting_info_trample, EnvironmentSetting.TRAMPLE);
		add_menu_item_setting(region_group_menu, group, 8, item_setting_info_vine_growth, EnvironmentSetting.VINE_GROWTH);

		region_group_menu.on_natural_close(player2 -> get_module().menus.main_menu.create(player2).open(player2));

		return region_group_menu;
	}

	private MenuWidget menu_item_rename(final RegionGroup group) {
		return new MenuItem(
			0,
			item_rename.item(),
			(player, menu, self) -> {
				menu.close(player);

				get_module()
					.menus.enter_region_group_name_menu.create(
						player,
						group.name(),
						(player2, name) -> {
							group.name(name);
							mark_persistent_storage_dirty();

							// Open new menu because of possibly changed title
							get_module().menus.region_group_menu.create(group, player2).open(player2);
							return ClickResult.SUCCESS;
						}
					)
					.on_natural_close(player2 -> {
						// Open new menu because of possibly changed title
						get_module().menus.region_group_menu.create(group, player2).open(player2);
					})
					.open(player);

				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_delete(final RegionGroup group) {
		final var orphan_checkbox = group.is_orphan(get_module()) ? "§a✓" : "§c✕";
		return new MenuItem(
			1,
			item_delete.item(orphan_checkbox),
			(player, menu, self) -> {
				if (!group.is_orphan(get_module())) {
					return ClickResult.ERROR;
				}

				menu.close(player);
				MenuFactory
					.confirm(
						get_context(),
						lang_delete_confirm_title.str(),
						item_delete_confirm_accept.item(),
						player2 -> {
							if (!player2.getUniqueId().equals(group.owner())) {
								return ClickResult.ERROR;
							}

							// Assert that this isn't the default group
							if (get_module().get_or_create_default_region_group(player2).id().equals(group.id())) {
								return ClickResult.ERROR;
							}

							get_module().remove_region_group(group);
							return ClickResult.SUCCESS;
						},
						item_delete_confirm_cancel.item(),
						player2 -> menu.open(player2)
					)
					.tag(new RegionGroupMenuTag(group.id()))
					.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_create_role(final RegionGroup group) {
		return new MenuItem(
			7,
			item_create_role.item(),
			(player, menu, self) -> {
				menu.close(player);
				get_module()
					.menus.enter_role_name_menu.create(
						player,
						(player2, name) -> {
							final var role = new Role(name, Role.RoleType.NORMAL);
							group.add_role(role);
							mark_persistent_storage_dirty();
							get_module().menus.role_menu.create(group, role, player).open(player);
							return ClickResult.SUCCESS;
						}
					)
					.on_natural_close(player2 -> menu.open(player2))
					.open(player);

				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_list_roles(final RegionGroup group) {
		return new MenuItem(
			8,
			item_list_roles.item(),
			(player, menu, self) -> {
				menu.close(player);
				final var all_roles = group
					.roles()
					.stream()
					.sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
					.collect(Collectors.toList());

				final var filter = new Filter.StringFilter<Role>((r, str) -> r.name().toLowerCase().contains(str));
				MenuFactory
					.generic_selector(
						get_context(),
						player,
						lang_select_role_title.str(),
						lang_filter_roles_title.str(),
						all_roles,
						r -> item_select_role.item(r.color() + "§l" + r.name()),
						filter,
						(player2, m, role) -> {
							m.close(player2);
							get_module().menus.role_menu.create(group, role, player2).open(player2);
							return ClickResult.SUCCESS;
						},
						player2 -> menu.open(player2)
					)
					.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private void add_menu_item_setting(
		final Menu region_group_menu,
		final RegionGroup group,
		final int col,
		final TranslatedItemStack<?> item_info,
		final EnvironmentSetting setting
	) {
		region_group_menu.add(
			new MenuItem(
				9 + col,
				item_info.item(),
				(player, menu, self) -> ClickResult.IGNORE
			)
		);

		region_group_menu.add(
			new MenuItem(
				2 * 9 + col,
				null,
				(player, menu, self) -> {
					// Prevent toggling when the setting is forced by the server
					if (setting.has_override()) {
						return ClickResult.ERROR;
					}

					group.settings().put(setting, !group.get_setting(setting));
					mark_persistent_storage_dirty();
					menu.update();
					return ClickResult.SUCCESS;
				}
			) {
				@Override
				public void item(final ItemStack item) {
					final Consumer<List<Component>> maybe_add_forced_hint = (lore) -> {
						if (setting.has_override()) {
							lore.add(Component.empty());
							lore.add(Component.text("FORCED BY SERVER"));
						}
					};

					if (group.get_setting(setting)) {
						super.item(item_setting_toggle_on.item_transform_lore(maybe_add_forced_hint));
					} else {
						super.item(item_setting_toggle_off.item_transform_lore(maybe_add_forced_hint));
					}
				}
			}
		);
	}

	@Override
	public void on_enable() {}

	@Override
	public void on_disable() {}
}
