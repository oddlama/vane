package org.oddlama.vane.portals.menu;

import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.StorageUtil;

public class ConsoleMenu extends ModuleComponent<Portals> {

	@LangMessage
	public TranslatedMessage lang_title;

	@LangMessage
	public TranslatedMessage lang_unlink_console_confirm_title;

	@LangMessage
	public TranslatedMessage lang_destroy_portal_confirm_title;

	@LangMessage
	public TranslatedMessage lang_select_target_title;

	@LangMessage
	public TranslatedMessage lang_filter_portals_title;

	@LangMessage
	public TranslatedMessage lang_select_target_portal_visibility_public;

	@LangMessage
	public TranslatedMessage lang_select_target_portal_visibility_private;

	@LangMessage
	public TranslatedMessage lang_select_target_portal_visibility_group;

	@LangMessage
	public TranslatedMessage lang_select_target_portal_visibility_group_internal;

	public TranslatedItemStack<?> item_settings;
	public TranslatedItemStack<?> item_select_target;
	public TranslatedItemStack<?> item_select_target_portal;
	public TranslatedItemStack<?> item_select_target_locked;
	public TranslatedItemStack<?> item_unlink_console;
	public TranslatedItemStack<?> item_unlink_console_confirm_accept;
	public TranslatedItemStack<?> item_unlink_console_confirm_cancel;
	public TranslatedItemStack<?> item_destroy_portal;
	public TranslatedItemStack<?> item_destroy_portal_confirm_accept;
	public TranslatedItemStack<?> item_destroy_portal_confirm_cancel;

	public ConsoleMenu(Context<Portals> context) {
		super(context.namespace("console"));
		final var ctx = get_context();
		item_settings =
			new TranslatedItemStack<>(ctx, "settings", Material.WRITABLE_BOOK, 1, "Used to enter portal settings.");
		item_select_target =
			new TranslatedItemStack<>(
				ctx,
				"select_target",
				Material.COMPASS,
				1,
				"Used to enter portal target selection."
			);
		item_select_target_portal =
			new TranslatedItemStack<>(
				ctx,
				"select_target_portal",
				Material.COMPASS,
				1,
				"Used to represent a portal in the target selection menu."
			);
		item_select_target_locked =
			new TranslatedItemStack<>(
				ctx,
				"select_target_locked",
				Material.FIREWORK_STAR,
				1,
				"Used to show portal target selection when the target is locked."
			);
		item_unlink_console =
			new TranslatedItemStack<>(
				ctx,
				"unlink_console",
				StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
				1,
				"Used to unlink the current console."
			);
		item_unlink_console_confirm_accept =
			new TranslatedItemStack<>(
				ctx,
				"unlink_console_confirm_accept",
				StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
				1,
				"Used to confirm unlinking the current console."
			);
		item_unlink_console_confirm_cancel =
			new TranslatedItemStack<>(
				ctx,
				"unlink_console_confirm_cancel",
				Material.PRISMARINE_SHARD,
				1,
				"Used to cancel unlinking the current console."
			);
		item_destroy_portal =
			new TranslatedItemStack<>(ctx, "destroy_portal", Material.TNT, 1, "Used to destroy the portal.");
		item_destroy_portal_confirm_accept =
			new TranslatedItemStack<>(
				ctx,
				"destroy_portal_confirm_accept",
				Material.TNT,
				1,
				"Used to confirm destroying the portal."
			);
		item_destroy_portal_confirm_cancel =
			new TranslatedItemStack<>(
				ctx,
				"destroy_portal_confirm_cancel",
				Material.PRISMARINE_SHARD,
				1,
				"Used to cancel destroying the portal."
			);
	}

	public Menu create(final Portal portal, final Player player, final Block console) {
		final var columns = 9;
		final var title = lang_title.str_component("§5§l" + portal.name());
		final var console_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));
		console_menu.tag(new PortalMenuTag(portal.id()));

		// Check if target selection would be allowed
		final var select_target_event = new PortalSelectTargetEvent(player, portal, null, true);
		get_module().getServer().getPluginManager().callEvent(select_target_event);
		if (!select_target_event.isCancelled() || player.hasPermission(get_module().admin_permission)) {
			console_menu.add(menu_item_select_target(portal));
		}

		// Check if settings would be allowed
		final var settings_event = new PortalChangeSettingsEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(settings_event);
		if (!settings_event.isCancelled() || player.hasPermission(get_module().admin_permission)) {
			console_menu.add(menu_item_settings(portal, console));
		}

		// Check if unlink would be allowed
		final var unlink_event = new PortalUnlinkConsoleEvent(player, console, portal, true);
		get_module().getServer().getPluginManager().callEvent(unlink_event);
		if (!unlink_event.isCancelled() || player.hasPermission(get_module().admin_permission)) {
			console_menu.add(menu_item_unlink_console(portal, console));
		}

		// Check if destroy would be allowed
		final var destroy_event = new PortalDestroyEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(destroy_event);
		if (!destroy_event.isCancelled() || player.hasPermission(get_module().admin_permission)) {
			console_menu.add(menu_item_destroy_portal(portal));
		}

		return console_menu;
	}

	private MenuWidget menu_item_settings(final Portal portal, final Block console) {
		return new MenuItem(
			0,
			item_settings.item(),
			(player, menu, self) -> {
				final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
				get_module().getServer().getPluginManager().callEvent(settings_event);
				if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
					get_module().lang_settings_restricted.send(player);
					return ClickResult.ERROR;
				}

				menu.close(player);
				get_module().menus.settings_menu.create(portal, player, console).open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private Component portal_visibility(Portal.Visibility visibility) {
		return (
			switch (visibility) {
				case PUBLIC -> lang_select_target_portal_visibility_public;
				case GROUP -> lang_select_target_portal_visibility_group;
				case GROUP_INTERNAL -> lang_select_target_portal_visibility_group_internal;
				case PRIVATE -> lang_select_target_portal_visibility_private;
			}
		).format();
	}

	private MenuWidget menu_item_select_target(final Portal portal) {
		return new MenuItem(
			4,
			null,
			(player, menu, self) -> {
				if (portal.target_locked()) {
					return ClickResult.ERROR;
				} else {
					menu.close(player);
					final var all_portals = get_module()
						.all_available_portals()
						.stream()
						.filter(p -> {
							switch (p.visibility()) {
								case PUBLIC:
									return true;
								case GROUP:
									return get_module().player_can_use_portals_in_region_group_of(player, p);
								case GROUP_INTERNAL:
									return get_module().is_in_same_region_group(portal, p);
								case PRIVATE:
									return player.getUniqueId().equals(p.owner());
							}
							return false;
						})
						.filter(p -> !Objects.equals(p.id(), portal.id()))
						.sorted(new Portal.TargetSelectionComparator(player))
						.collect(Collectors.toList());

					final var filter = new Filter.StringFilter<Portal>((p, str) -> p.name().toLowerCase().contains(str)
					);
					MenuFactory
						.generic_selector(
							get_context(),
							player,
							lang_select_target_title.str(),
							lang_filter_portals_title.str(),
							all_portals,
							p -> {
								final var dist = p
									.spawn()
									.toVector()
									.setY(0.0)
									.distance(player.getLocation().toVector().setY(0.0));
								return item_select_target_portal.alternative(
									get_module().icon_for(p),
									"§a§l" + p.name(),
									"§6" + String.format("%.1f", dist),
									"§b" + p.spawn().getWorld().getName(),
									portal_visibility(p.visibility())
								);
							},
							filter,
							(player2, m, t) -> {
								m.close(player2);

								final var select_target_event = new PortalSelectTargetEvent(player2, portal, t, false);
								get_module().getServer().getPluginManager().callEvent(select_target_event);
								if (
									select_target_event.isCancelled() &&
									!player2.hasPermission(get_module().admin_permission)
								) {
									get_module().lang_select_target_restricted.send(player2);
									return ClickResult.ERROR;
								}

								portal.target_id(t.id());

								// Update portal block to reflect new target on consoles
								portal.update_blocks(get_module());
								return ClickResult.SUCCESS;
							},
							player2 -> menu.open(player2)
						)
						.tag(new PortalMenuTag(portal.id()))
						.open(player);
					return ClickResult.SUCCESS;
				}
			}
		) {
			@Override
			public void item(final ItemStack item) {
				final var target = portal.target(get_module());
				final var target_name = "§a" + (target == null ? "None" : target.name());
				if (portal.target_locked()) {
					super.item(item_select_target_locked.item(target_name));
				} else {
					super.item(item_select_target.item(target_name));
				}
			}
		};
	}

	private MenuWidget menu_item_unlink_console(final Portal portal, final Block console) {
		return new MenuItem(
			7,
			item_unlink_console.item(),
			(player, menu, self) -> {
				menu.close(player);
				MenuFactory
					.confirm(
						get_context(),
						lang_unlink_console_confirm_title.str(),
						item_unlink_console_confirm_accept.item(),
						player2 -> {
							// Call event
							final var event = new PortalUnlinkConsoleEvent(player2, console, portal, false);
							get_module().getServer().getPluginManager().callEvent(event);
							if (event.isCancelled() && !player2.hasPermission(get_module().admin_permission)) {
								get_module().lang_unlink_restricted.send(player2);
								return ClickResult.ERROR;
							}

							final var portal_block = portal.portal_block_for(console);
							if (portal_block == null) {
								// Console was likely already removed by another player
								return ClickResult.ERROR;
							}

							get_module().remove_portal_block(portal, portal_block);
							return ClickResult.SUCCESS;
						},
						item_unlink_console_confirm_cancel.item(),
						player2 -> menu.open(player2)
					)
					.tag(new PortalMenuTag(portal.id()))
					.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_destroy_portal(final Portal portal) {
		return new MenuItem(
			8,
			item_destroy_portal.item(),
			(player, menu, self) -> {
				menu.close(player);
				MenuFactory
					.confirm(
						get_context(),
						lang_destroy_portal_confirm_title.str(),
						item_destroy_portal_confirm_accept.item(),
						player2 -> {
							// Call event
							final var event = new PortalDestroyEvent(player2, portal, false);
							get_module().getServer().getPluginManager().callEvent(event);
							if (event.isCancelled() && !player2.hasPermission(get_module().admin_permission)) {
								get_module().lang_destroy_restricted.send(player2);
								return ClickResult.ERROR;
							}

							get_module().remove_portal(portal);
							return ClickResult.SUCCESS;
						},
						item_destroy_portal_confirm_cancel.item(),
						player2 -> menu.open(player2)
					)
					.tag(new PortalMenuTag(portal.id()))
					.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	@Override
	public void on_enable() {}

	@Override
	public void on_disable() {}
}
