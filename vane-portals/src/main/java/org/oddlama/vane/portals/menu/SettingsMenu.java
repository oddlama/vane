package org.oddlama.vane.portals.menu;

import static org.oddlama.vane.util.Util.namespaced_key;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
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

public class SettingsMenu extends ModuleComponent<Portals> {
	@LangMessage public TranslatedMessage lang_title;
	@LangMessage public TranslatedMessage lang_select_icon_title;

	public TranslatedItemStack<?> item_rename;
	public TranslatedItemStack<?> item_select_icon;
	public TranslatedItemStack<?> item_select_style;
	public TranslatedItemStack<?> item_visibility_public;
	public TranslatedItemStack<?> item_visibility_group;
	public TranslatedItemStack<?> item_visibility_private;
	public TranslatedItemStack<?> item_target_lock_on;
	public TranslatedItemStack<?> item_target_lock_off;
	public TranslatedItemStack<?> item_back;

	public SettingsMenu(Context<Portals> context) {
		super(context.namespace("settings"));

		final var ctx = get_context();
        item_rename             = new TranslatedItemStack<>(ctx, "rename",             Material.WRITABLE_BOOK,                              1, "Used to TODO.");
        item_select_icon        = new TranslatedItemStack<>(ctx, "select_icon",        namespaced_key("vane", "decoration_end_portal_orb"), 1, "Used to TODO.");
        item_select_style       = new TranslatedItemStack<>(ctx, "select_style",       Material.ITEM_FRAME,                                 1, "Used to TODO.");
        item_visibility_public  = new TranslatedItemStack<>(ctx, "visibility_public",  Material.ENDER_EYE,                                  1, "Used to TODO.");
        item_visibility_group   = new TranslatedItemStack<>(ctx, "visibility_group",   Material.ENDER_PEARL,                                1, "Used to TODO.");
        item_visibility_private = new TranslatedItemStack<>(ctx, "visibility_private", Material.FIREWORK_STAR,                              1, "Used to TODO.");
        item_target_lock_on     = new TranslatedItemStack<>(ctx, "target_lock_on",     Material.SLIME_BALL,                                 1, "Used to TODO.");
        item_target_lock_off    = new TranslatedItemStack<>(ctx, "target_lock_off",    Material.SNOWBALL,                                   1, "Used to TODO.");
        item_back               = new TranslatedItemStack<>(ctx, "back",               Material.PRISMARINE_SHARD,                           1, "Used to TODO.");
	}

	public Menu create(final Portal portal, final Player player, final Menu previous) {
		final var columns = 9;
		final var title = lang_title.str("§5§l" + portal.name());
		final var settings_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));

		settings_menu.add(menu_item_rename(portal, previous));
		settings_menu.add(menu_item_select_icon(portal));
		settings_menu.add(menu_item_select_style(portal));
		settings_menu.add(menu_item_visibility(portal));
		settings_menu.add(menu_item_target_lock(portal));
		settings_menu.add(menu_item_back(previous));

		return settings_menu;
	}

	private MenuWidget menu_item_rename(final Portal portal, final Menu previous) {
		return new MenuItem(0, item_rename.item(), (player, menu, self) -> {
			menu.close(player);
			// TODO
			// Open new menu because of changed title
			get_module().menus.settings_menu.create(portal, player, previous).open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_select_icon(final Portal portal) {
		return new MenuItem(1, item_select_icon.item(), (player, menu, self) -> {
			menu.close(player);
			MenuFactory.item_selector(get_context(), player, lang_select_icon_title.str(), portal.icon(), true, (player2, item) -> {
				// TODO permission
				portal.icon(item);
				get_module().update_portal_icon(portal);
				mark_persistent_storage_dirty();
				menu.open(player2);
			}, player2 -> {
				menu.open(player2);
			}).open(player);
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_select_style(final Portal portal) {
		return new MenuItem(2, item_select_style.item(), (player, menu, self) -> {
			menu.close(player);
			// TODO
			return ClickResult.SUCCESS;
		});
	}

	private MenuWidget menu_item_visibility(final Portal portal) {
		return new MenuItem(5, null, (player, menu, self, type, action) -> {
			if (!Menu.is_left_or_right_click(type, action)) {
				return ClickResult.INVALID_CLICK;
			}

			portal.visibility(type == ClickType.RIGHT ? portal.visibility().prev() : portal.visibility().next());
			get_module().update_portal_visibility(portal);
			mark_persistent_storage_dirty();
			menu.update();
			return ClickResult.SUCCESS;
		}) {
			@Override
			public void item(final ItemStack item) {
				switch (portal.visibility()) {
					case PUBLIC:  super.item(item_visibility_public.item());  break;
					case GROUP:   super.item(item_visibility_group.item());   break;
					case PRIVATE: super.item(item_visibility_private.item()); break;
				}
			}
		};
	}

	private MenuWidget menu_item_target_lock(final Portal portal) {
		return new MenuItem(6, null, (player, menu, self) -> {
			portal.target_locked(!portal.target_locked());
			mark_persistent_storage_dirty();
			menu.update();
			return ClickResult.SUCCESS;
		}) {
			@Override
			public void item(final ItemStack item) {
				if (portal.target_locked()) {
					super.item(item_target_lock_on.item());
				} else {
					super.item(item_target_lock_off.item());
				}
			}
		};
	}

	private MenuWidget menu_item_back(final Menu previous) {
		return new MenuItem(8, item_back.item(), (player, menu, self) -> {
			menu.close(player);
			previous.open(player);
			return ClickResult.SUCCESS;
		});
	}

	@Override public void on_enable() {}
	@Override public void on_disable() {}
}
