package org.oddlama.vane.portals.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.StorageUtil;

public class SettingsMenu extends ModuleComponent<Portals> {

    @LangMessage
    public TranslatedMessage lang_title;

    @LangMessage
    public TranslatedMessage lang_select_icon_title;

    public TranslatedItemStack<?> item_rename;
    public TranslatedItemStack<?> item_select_icon;
    public TranslatedItemStack<?> item_select_style;
    public TranslatedItemStack<?> item_exit_orientation_lock_on;
    public TranslatedItemStack<?> item_exit_orientation_lock_off;
    public TranslatedItemStack<?> item_visibility_public;
    public TranslatedItemStack<?> item_visibility_group;
    public TranslatedItemStack<?> item_visibility_group_internal;
    public TranslatedItemStack<?> item_visibility_private;
    public TranslatedItemStack<?> item_target_lock_on;
    public TranslatedItemStack<?> item_target_lock_off;
    public TranslatedItemStack<?> item_back;

    public SettingsMenu(Context<Portals> context) {
        super(context.namespace("settings"));
        final var ctx = get_context();
        item_rename = new TranslatedItemStack<>(ctx, "rename", Material.NAME_TAG, 1, "Used to rename the portal.");
        item_select_icon = new TranslatedItemStack<>(
            ctx,
            "select_icon",
            StorageUtil.namespaced_key("vane", "decoration_end_portal_orb"),
            1,
            "Used to select the portal's icon."
        );
        item_select_style = new TranslatedItemStack<>(
            ctx,
            "select_style",
            Material.ITEM_FRAME,
            1,
            "Used to change the portal's style."
        );
        item_exit_orientation_lock_on = new TranslatedItemStack<>(
            ctx,
            "exit_orientation_lock_on",
            Material.SOUL_TORCH,
            1,
            "Used to toggle and indicate enabled exit orientation lock."
        );
        item_exit_orientation_lock_off = new TranslatedItemStack<>(
            ctx,
            "exit_orientation_lock_off",
            Material.TORCH,
            1,
            "Used to toggle and indicate disabled exit orientation lock."
        );
        item_visibility_public = new TranslatedItemStack<>(
            ctx,
            "visibility_public",
            Material.ENDER_EYE,
            1,
            "Used to change and indicate public visibility."
        );
        item_visibility_group = new TranslatedItemStack<>(
            ctx,
            "visibility_group",
            Material.ENDER_PEARL,
            1,
            "Used to change and indicate group visibility."
        );
        item_visibility_group_internal = new TranslatedItemStack<>(
            ctx,
            "visibility_group_internal",
            Material.FIRE_CHARGE,
            1,
            "Used to change and indicate group internal visibility."
        );
        item_visibility_private = new TranslatedItemStack<>(
            ctx,
            "visibility_private",
            Material.FIREWORK_STAR,
            1,
            "Used to change and indicate private visibility."
        );
        item_target_lock_on = new TranslatedItemStack<>(
            ctx,
            "target_lock_on",
            Material.SLIME_BALL,
            1,
            "Used to toggle and indicate enabled target lock."
        );
        item_target_lock_off = new TranslatedItemStack<>(
            ctx,
            "target_lock_off",
            Material.SNOWBALL,
            1,
            "Used to toggle and indicate disabled target lock."
        );
        item_back = new TranslatedItemStack<>(
            ctx,
            "back",
            Material.PRISMARINE_SHARD,
            1,
            "Used to go back to the previous menu."
        );
    }

    // HINT: We don't capture the previous menu and open a new one on exit,
    // to correctly reflect changes done in here. (e.g., menu title due to portal name)
    public Menu create(final Portal portal, final Player player, final Block console) {
        final var columns = 9;
        final var title = lang_title.str_component("§5§l" + portal.name());
        final var settings_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));
        settings_menu.tag(new PortalMenuTag(portal.id()));

        settings_menu.add(menu_item_rename(portal, console));
        settings_menu.add(menu_item_select_icon(portal));
        settings_menu.add(menu_item_select_style(portal));
        settings_menu.add(menu_item_exit_orientation_lock(portal));
        settings_menu.add(menu_item_visibility(portal));
        settings_menu.add(menu_item_target_lock(portal));
        settings_menu.add(menu_item_back(portal, console));

        settings_menu.on_natural_close(player2 ->
            get_module().menus.console_menu.create(portal, player2, console).open(player2)
        );

        return settings_menu;
    }

    private MenuWidget menu_item_rename(final Portal portal, final Block console) {
        return new MenuItem(0, item_rename.item(), (player, menu, self) -> {
            menu.close(player);

            get_module()
                .menus.enter_name_menu.create(player, portal.name(), (player2, name) -> {
                    final var settings_event = new PortalChangeSettingsEvent(player2, portal, false);
                    get_module().getServer().getPluginManager().callEvent(settings_event);
                    if (settings_event.isCancelled() && !player2.hasPermission(get_module().admin_permission)) {
                        get_module().lang_settings_restricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    portal.name(name);

                    // Update portal icons to reflect new name
                    get_module().update_portal_icon(portal);

                    // Open new menu because of possibly changed title
                    get_module().menus.settings_menu.create(portal, player2, console).open(player2);
                    return ClickResult.SUCCESS;
                })
                .on_natural_close(player2 -> {
                    // Open new menu because of possibly changed title
                    get_module().menus.settings_menu.create(portal, player2, console).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menu_item_select_icon(final Portal portal) {
        return new MenuItem(1, item_select_icon.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.item_selector(
                get_context(),
                player,
                lang_select_icon_title.str(),
                portal.icon(),
                true,
                (player2, item) -> {
                    final var settings_event = new PortalChangeSettingsEvent(player2, portal, false);
                    get_module().getServer().getPluginManager().callEvent(settings_event);
                    if (settings_event.isCancelled() && !player2.hasPermission(get_module().admin_permission)) {
                        get_module().lang_settings_restricted.send(player2);
                        return ClickResult.ERROR;
                    }

                    portal.icon(item);
                    get_module().update_portal_icon(portal);
                    menu.open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            )
                .tag(new PortalMenuTag(portal.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menu_item_select_style(final Portal portal) {
        return new MenuItem(2, item_select_style.item(), (player, menu, self) -> {
            final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
            get_module().getServer().getPluginManager().callEvent(settings_event);
            if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
                get_module().lang_settings_restricted.send(player);
                return ClickResult.ERROR;
            }

            menu.close(player);
            get_module().menus.style_menu.create(portal, player, menu).open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menu_item_exit_orientation_lock(final Portal portal) {
        return new MenuItem(4, null, (player, menu, self) -> {
            final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
            get_module().getServer().getPluginManager().callEvent(settings_event);
            if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
                get_module().lang_settings_restricted.send(player);
                return ClickResult.ERROR;
            }

            portal.exit_orientation_locked(!portal.exit_orientation_locked());
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                if (portal.exit_orientation_locked()) {
                    super.item(item_exit_orientation_lock_on.item());
                } else {
                    super.item(item_exit_orientation_lock_off.item());
                }
            }
        };
    }

    private MenuWidget menu_item_visibility(final Portal portal) {
        return new MenuItem(5, null, (player, menu, self, event) -> {
            if (!Menu.is_left_or_right_click(event)) {
                return ClickResult.INVALID_CLICK;
            }

            final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
            get_module().getServer().getPluginManager().callEvent(settings_event);
            if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
                get_module().lang_settings_restricted.send(player);
                return ClickResult.ERROR;
            }

            Portal.Visibility new_vis = portal.visibility();
            // If the "regions" plugin is not installed, we need to skip group visibility.
            do {
                new_vis = event.getClick() == ClickType.RIGHT ? new_vis.prev() : new_vis.next();
            } while (new_vis.requires_regions() && !get_module().is_regions_installed());

            portal.visibility(new_vis);
            get_module().update_portal_visibility(portal);
            menu.update();
            return ClickResult.SUCCESS;
        }) {
            @Override
            public void item(final ItemStack item) {
                switch (portal.visibility()) {
                    case PUBLIC:
                        super.item(item_visibility_public.item());
                        break;
                    case GROUP:
                        super.item(item_visibility_group.item());
                        break;
                    case GROUP_INTERNAL:
                        super.item(item_visibility_group_internal.item());
                        break;
                    case PRIVATE:
                        super.item(item_visibility_private.item());
                        break;
                }
            }
        };
    }

    private MenuWidget menu_item_target_lock(final Portal portal) {
        return new MenuItem(6, null, (player, menu, self) -> {
            final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
            get_module().getServer().getPluginManager().callEvent(settings_event);
            if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
                get_module().lang_settings_restricted.send(player);
                return ClickResult.ERROR;
            }

            portal.target_locked(!portal.target_locked());
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

    private MenuWidget menu_item_back(final Portal portal, final Block console) {
        return new MenuItem(8, item_back.item(), (player, menu, self) -> {
            menu.close(player);
            get_module().menus.console_menu.create(portal, player, console).open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
