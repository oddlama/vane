package org.oddlama.vane.core.menu;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.module.Context;

public class MenuManager extends Listener<Core> {

    private final HashMap<UUID, Menu> open_menus = new HashMap<>();
    private final HashMap<Inventory, Menu> menus = new HashMap<>();

    public TranslatedItemStack<?> item_selector_accept;
    public TranslatedItemStack<?> item_selector_cancel;
    public TranslatedItemStack<?> item_selector_selected;

    public TranslatedItemStack<?> generic_selector_page;
    public TranslatedItemStack<?> generic_selector_current_page;
    public TranslatedItemStack<?> generic_selector_filter;
    public TranslatedItemStack<?> generic_selector_cancel;

    public HeadSelectorGroup head_selector;

    public MenuManager(Context<Core> context) {
        super(context.namespace("menus"));
        final var ctx = get_context();
        head_selector = new HeadSelectorGroup(ctx);

        final var ctx_item_selector = ctx.namespace("item_selector", "Menu configuration for item selector menus.");
        item_selector_accept = new TranslatedItemStack<>(
            ctx_item_selector,
            "accept",
            Material.LIME_TERRACOTTA,
            1,
            "Used to confirm item selection."
        );
        item_selector_cancel = new TranslatedItemStack<>(
            ctx_item_selector,
            "cancel",
            Material.RED_TERRACOTTA,
            1,
            "Used to cancel item selection."
        );
        item_selector_selected = new TranslatedItemStack<>(
            ctx_item_selector,
            "selected",
            Material.BARRIER,
            1,
            "Represents the selected item. Left-clicking will reset the selection to the initial value, and right-clicking will clear the selected item. The given stack is used as the 'empty', cleared item."
        );

        final var ctx_generic_selector = ctx.namespace(
            "generic_selector",
            "Menu configuration for generic selector menus."
        );
        generic_selector_page = new TranslatedItemStack<>(
            ctx_generic_selector,
            "page",
            Material.PAPER,
            1,
            "Used to select pages."
        );
        generic_selector_current_page = new TranslatedItemStack<>(
            ctx_generic_selector,
            "current_page",
            Material.MAP,
            1,
            "Used to indicate current page."
        );
        generic_selector_filter = new TranslatedItemStack<>(
            ctx_generic_selector,
            "filter",
            Material.HOPPER,
            1,
            "Used to filter items."
        );
        generic_selector_cancel = new TranslatedItemStack<>(
            ctx_generic_selector,
            "cancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel selection."
        );
    }

    public Menu menu_for(final Player player, final InventoryView view) {
        return menu_for(player, view.getTopInventory());
    }

    public Menu menu_for(final Player player, final Inventory inventory) {
        final var menu = menus.get(inventory);
        final var open = open_menus.get(player.getUniqueId());
        if (open != menu && menu != null) {
            get_module()
                .log.warning(
                    "Menu inconsistency: entity " +
                    player +
                    " accessed a menu '" +
                    open_menus.get(player.getUniqueId()) +
                    "' that isn't registered to it. The registered menu is '" +
                    menu +
                    "'"
                );
            return menu;
        }
        return menu == null ? open : menu;
    }

    public void add(final Player player, final Menu menu) {
        open_menus.put(player.getUniqueId(), menu);
        menus.put(menu.inventory(), menu);
    }

    public void remove(final Player player, final Menu menu) {
        open_menus.remove(player.getUniqueId());
        final var orphaned = open_menus.values().stream().allMatch(m -> m != menu);

        // Remove orphaned menus from other maps
        if (orphaned) {
            menus.remove(menu.inventory());
        }
    }

    public void for_each_open(final Consumer2<Player, Menu> functor) {
        for (final var player : get_module().getServer().getOnlinePlayers()) {
            final var open = open_menus.get(player.getUniqueId());
            if (open == null) {
                continue;
            }

            functor.apply(player, open);
        }
    }

    public void update(final Menu menu) {
        get_module()
            .getServer()
            .getOnlinePlayers()
            .stream()
            .filter(p -> open_menus.get(p.getUniqueId()) == menu)
            .forEach(p -> p.updateInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_inventory_click(final InventoryClickEvent event) {
        final var clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }

        final var player = (Player) clicker;
        final var menu = menu_for(player, event.getView());
        if (menu != null) {
            event.setCancelled(true);
            final var slot = event.getClickedInventory() == menu.inventory() ? event.getSlot() : -1;
            menu.click(player, event.getCurrentItem(), slot, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_inventory_drag(final InventoryDragEvent event) {
        final var clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }

        final var player = (Player) clicker;
        final var menu = menu_for(player, event.getView());
        if (menu != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_inventory_close(final InventoryCloseEvent event) {
        final var human = event.getPlayer();
        if (!(human instanceof Player)) {
            return;
        }

        final var player = (Player) human;
        final var menu = menu_for(player, event.getView());
        if (menu != null) {
            menu.closed(player, event.getReason());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on_prepare_anvil_event(final PrepareAnvilEvent event) {
        final var menu = menus.get(event.getView().getTopInventory());
        if (menu != null) {
            event.getView().setRepairCost(0);
        }
    }
}
