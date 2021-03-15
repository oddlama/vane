package org.oddlama.vane.regions.menu;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.OfflinePlayer;
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
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.util.ItemUtil;

public class RoleMenu extends ModuleComponent<Regions> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_delete_confirm_title;
	@LangMessage public TranslatedMessage lang_select_assign_player_title;
	@LangMessage public TranslatedMessage lang_select_remove_player_title;
	@LangMessage public TranslatedMessage lang_filter_players_title;

	public TranslatedItemStack<?> item_rename;
	public TranslatedItemStack<?> item_delete;
	public TranslatedItemStack<?> item_delete_confirm_accept;
	public TranslatedItemStack<?> item_delete_confirm_cancel;
	public TranslatedItemStack<?> item_assign_player;
	public TranslatedItemStack<?> item_remove_player;
	public TranslatedItemStack<?> item_select_player;

	public RoleMenu(Context<Regions> context) {
		super(context.namespace("role"));

		final var ctx = get_context();
        item_rename                = new TranslatedItemStack<>(ctx, "rename",                Material.NAME_TAG,                          1, "Used to rename the role.");
        item_delete                = new TranslatedItemStack<>(ctx, "delete",                namespaced_key("vane", "decoration_tnt_1"), 1, "Used to delete this role.");
        item_delete_confirm_accept = new TranslatedItemStack<>(ctx, "delete_confirm_accept", namespaced_key("vane", "decoration_tnt_1"), 1, "Used to confirm deleting the role.");
        item_delete_confirm_cancel = new TranslatedItemStack<>(ctx, "delete_confirm_cancel", Material.PRISMARINE_SHARD,                  1, "Used to cancel deleting the role.");
        item_assign_player         = new TranslatedItemStack<>(ctx, "assign_player",         Material.PLAYER_HEAD,                       1, "Used to assign players to this role.");
        item_remove_player         = new TranslatedItemStack<>(ctx, "remove_player",         Material.PLAYER_HEAD,                       1, "Used to remove players from this role.");
        item_select_player         = new TranslatedItemStack<>(ctx, "select_player",         Material.PLAYER_HEAD,                       1, "Used to represent a player in the role assignment/removal list.");
	}

	public Menu create(final RegionGroup group, final Role role, final Player player) {
		final var columns = 9;
		final var title = lang_title.str(role.name());
		final var role_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));

		final var is_admin = player.getUniqueId().equals(group.owner())
			|| group.get_role(player.getUniqueId())
			        .get_setting(RoleSetting.ADMIN);

		if (is_admin && role.role_type() == Role.RoleType.NORMAL) {
			role_menu.add(menu_item_rename(group, role));
			role_menu.add(menu_item_delete(group, role));
		}

		if (role.role_type() != Role.RoleType.OTHERS) {
			role_menu.add(menu_item_assign_player(group, role));
			role_menu.add(menu_item_remove_player(group, role));
		}
		// TODO integrate settings directly

		role_menu.on_natural_close(player2 ->
			get_module().menus.region_group_menu
				.create(group, player2)
				.open(player2));

		return role_menu;
	}

	private MenuWidget menu_item_rename(final RegionGroup group, final Role role) {
		return new MenuItem(0, item_rename.item(), (player, menu, self) -> {
			menu.close(player);

			get_module().menus.enter_role_name_menu.create(player, role.name(), (player2, name) -> {
				role.name(name);
				mark_persistent_storage_dirty();

				// Open new menu because of possibly changed title
				get_module().menus.role_menu.create(group, role, player2).open(player2);
				return ClickResult.SUCCESS;
			}).on_natural_close(player2 -> {
				// Open new menu because of possibly changed title
				get_module().menus.role_menu.create(group, role, player2).open(player2);
			}).open(player);

			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_delete(final RegionGroup group, final Role role) {
		return new MenuItem(1, item_delete.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.confirm(get_context(), lang_delete_confirm_title.str(),
				item_delete_confirm_accept.item(), (player2) -> {
					group.remove_role(role.id());
					mark_persistent_storage_dirty();
					return ClickResult.SUCCESS;
				}, item_delete_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
				.open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_assign_player(final RegionGroup group, final Role role) {
		return new MenuItem(7, item_assign_player.item(), (player, menu, self) -> {
			menu.close(player);
			final var all_players = Arrays.stream(get_module().getServer().getOfflinePlayers())
				.filter(p -> !role.id().equals(group.player_to_role().get(p.getUniqueId())))
				.sorted((a, b) -> {
					int c = Boolean.compare(b.isOnline(), a.isOnline());
					if (c != 0) { return c; }
					return a.getName().compareToIgnoreCase(b.getName());
				})
				.collect(Collectors.toList());

			final var filter = new Filter.StringFilter<OfflinePlayer>((p, str) -> p.getName().toLowerCase().contains(str));
			MenuFactory.generic_selector(get_context(), player, lang_select_assign_player_title.str(), lang_filter_players_title.str(), all_players,
				p -> item_select_player.alternative(ItemUtil.skull_for_player(p), "§a§l" + p.getName()),
				filter,
				(player2, m, p) -> {
					all_players.remove(p);
					m.update();
					// TODO assing
					return ClickResult.SUCCESS;
				}, player2 -> {
					menu.open(player2);
				}).open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_remove_player(final RegionGroup group, final Role role) {
		return new MenuItem(8, item_remove_player.item(), (player, menu, self) -> {
			menu.close(player);
			final var all_players = Arrays.stream(get_module().getServer().getOfflinePlayers())
				.filter(p -> role.id().equals(group.player_to_role().get(p.getUniqueId())))
				.sorted((a, b) -> {
					int c = Boolean.compare(b.isOnline(), a.isOnline());
					if (c != 0) { return c; }
					return a.getName().compareToIgnoreCase(b.getName());
				})
				.collect(Collectors.toList());

			final var filter = new Filter.StringFilter<OfflinePlayer>((p, str) -> p.getName().toLowerCase().contains(str));
			MenuFactory.generic_selector(get_context(), player, lang_select_remove_player_title.str(), lang_filter_players_title.str(), all_players,
				p -> item_select_player.alternative(ItemUtil.skull_for_player(p), "§a§l" + p.getName()),
				filter,
				(player2, m, p) -> {
					all_players.remove(p);
					m.update();
					// TODO remove
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
