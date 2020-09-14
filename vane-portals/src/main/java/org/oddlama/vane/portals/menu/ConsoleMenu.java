package org.oddlama.vane.portals.menu;

import static org.oddlama.vane.util.Util.namespaced_key;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;

public class ConsoleMenu extends ModuleComponent<Portals> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_unlink_console_confirm_title;
	@LangMessage public TranslatedMessage lang_destroy_portal_confirm_title;

	public TranslatedItemStack<?> item_settings;
	public TranslatedItemStack<?> item_select_target;
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
        item_settings                      = new TranslatedItemStack<>(ctx, "settings",                      Material.WRITABLE_BOOK,                     1, "Used to enter portal settings.");
        item_select_target                 = new TranslatedItemStack<>(ctx, "select_target",                 Material.COMPASS,                           1, "Used to enter portal target selection.");
        item_select_target_locked          = new TranslatedItemStack<>(ctx, "select_target_locked",          Material.FIREWORK_STAR,                     1, "Used to show portal target selection when the target is locked.");
        item_unlink_console                = new TranslatedItemStack<>(ctx, "unlink_console",                namespaced_key("vane", "decoration_tnt_1"), 1, "Used to unlink the current console.");
        item_unlink_console_confirm_accept = new TranslatedItemStack<>(ctx, "unlink_console_confirm_accept", namespaced_key("vane", "decoration_tnt_1"), 1, "Used to confirm unlinking the current console.");
        item_unlink_console_confirm_cancel = new TranslatedItemStack<>(ctx, "unlink_console_confirm_cancel", Material.PRISMARINE_SHARD,                  1, "Used to cancel unlinking the current console.");
        item_destroy_portal                = new TranslatedItemStack<>(ctx, "destroy_portal",                Material.TNT,                               1, "Used to destroy the portal.");
        item_destroy_portal_confirm_accept = new TranslatedItemStack<>(ctx, "destroy_portal_confirm_accept", Material.TNT,                               1, "Used to confirm destroying the portal.");
        item_destroy_portal_confirm_cancel = new TranslatedItemStack<>(ctx, "destroy_portal_confirm_cancel", Material.PRISMARINE_SHARD,                  1, "Used to cancel destroying the portal.");
	}

	public Menu create(final Portal portal, final Player player, final Block console) {
		final var columns = 9;
		final var title = lang_title.str("§5§l" + portal.name());
		final var console_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));

		// Check if target selection would be allowed
		final var select_target_event = new PortalSelectTargetEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(select_target_event);
		if (!select_target_event.isCancelled()) {
			console_menu.add(menu_item_select_target(portal));
		}

		// Check if settings would be allowed
		final var settings_event = new PortalChangeSettingsEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(settings_event);
		if (!settings_event.isCancelled()) {
			console_menu.add(menu_item_settings(portal));
		}

		// Check if unlink would be allowed
		final var unlink_event = new PortalUnlinkConsoleEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(unlink_event);
		if (!unlink_event.isCancelled()) {
			console_menu.add(menu_item_unlink_console(portal, console));
		}

		// Check if destroy would be allowed
		final var destroy_event = new PortalDestroyEvent(player, portal, true);
		get_module().getServer().getPluginManager().callEvent(destroy_event);
		if (!destroy_event.isCancelled()) {
			console_menu.add(menu_item_destroy_portal(portal));
		}

		return console_menu;
	}

	private MenuWidget menu_item_settings(final Portal portal) {
		return new MenuItem(0, item_settings.item(), (player, menu, self) -> {
			menu.close(player);
			get_module().menus.settings_menu.create(portal, player, menu).open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_select_target(final Portal portal) {
		return new MenuItem(4, null, (player, menu, self) -> {
			if (portal.target_locked()) {
				return ClickResult.ERROR;
			} else {
				menu.close(player);
				// TODO target sel
				return ClickResult.SUCCESS;
			}
		}) {
			@Override
			public void item(final ItemStack item) {
				if (portal.target_locked()) {
					super.item(item_select_target_locked.item());
				} else {
					super.item(item_select_target.item());
				}
			}
		};
	}

	private MenuWidget menu_item_unlink_console(final Portal portal, final Block console) {
		return new MenuItem(7, item_unlink_console.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.confirm(get_context(), lang_unlink_console_confirm_title.str(),
				item_unlink_console_confirm_accept.item(), (player2) -> {
					// Call event
					final var event = new PortalUnlinkConsoleEvent(player2, portal, false);
					get_module().getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						get_module().lang_unlink_restricted.send(player2);
						return;
					}

					final var portal_block = portal.portal_block_for(console);
					if (portal_block == null) {
						// Console was likely already removed by another player
						return;
					}

					get_module().remove_portal_block(portal, portal_block);
				}, item_unlink_console_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
			.open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_destroy_portal(final Portal portal) {
		return new MenuItem(8, item_destroy_portal.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.confirm(get_context(), lang_destroy_portal_confirm_title.str(),
				item_destroy_portal_confirm_accept.item(), (player2) -> {
					// Call event
					final var event = new PortalDestroyEvent(player2, portal, false);
					get_module().getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						get_module().lang_destroy_restricted.send(player2);
						return;
					}

					get_module().remove_portal(portal);
				}, item_destroy_portal_confirm_cancel.item(), (player2) -> {
					menu.open(player2);
				})
			.open(player);
			return ClickResult.SUCCESS;
		});
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
