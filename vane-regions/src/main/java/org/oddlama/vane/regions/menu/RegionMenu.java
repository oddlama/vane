package org.oddlama.vane.regions.menu;

import static org.oddlama.vane.util.PlayerUtil.give_items;

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
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.util.StorageUtil;

public class RegionMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage lang_title;

    @LangMessage
    public TranslatedMessage lang_delete_confirm_title;

    @LangMessage
    public TranslatedMessage lang_select_region_group_title;

    @LangMessage
    public TranslatedMessage lang_filter_region_groups_title;

    public TranslatedItemStack<?> item_rename;
    public TranslatedItemStack<?> item_delete;
    public TranslatedItemStack<?> item_delete_confirm_accept;
    public TranslatedItemStack<?> item_delete_confirm_cancel;
    public TranslatedItemStack<?> item_assign_region_group;
    public TranslatedItemStack<?> item_select_region_group;

    public RegionMenu(Context<Regions> context) {
        super(context.namespace("region"));
        final var ctx = get_context();
        item_rename = new TranslatedItemStack<>(ctx, "rename", Material.NAME_TAG, 1, "Used to rename the region.");
        item_delete = new TranslatedItemStack<>(
            ctx,
            "delete",
            StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
            1,
            "Used to delete this region."
        );
        item_delete_confirm_accept = new TranslatedItemStack<>(
            ctx,
            "delete_confirm_accept",
            StorageUtil.namespaced_key("vane", "decoration_tnt_1"),
            1,
            "Used to confirm deleting the region."
        );
        item_delete_confirm_cancel = new TranslatedItemStack<>(
            ctx,
            "delete_confirm_cancel",
            Material.PRISMARINE_SHARD,
            1,
            "Used to cancel deleting the region."
        );
        item_assign_region_group = new TranslatedItemStack<>(
            ctx,
            "assign_region_group",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to assign a region group."
        );
        item_select_region_group = new TranslatedItemStack<>(
            ctx,
            "select_region_group",
            Material.GLOBE_BANNER_PATTERN,
            1,
            "Used to represent a region group in the region group assignment list."
        );
    }

    public Menu create(final Region region, final Player player) {
        final var columns = 9;
        final var title = lang_title.str_component("§5§l" + region.name());
        final var region_menu = new Menu(get_context(), Bukkit.createInventory(null, columns, title));
        region_menu.tag(new RegionMenuTag(region.id()));

        if (get_module().may_administrate(player, region)) {
            region_menu.add(menu_item_rename(region));
            region_menu.add(menu_item_delete(region));
            region_menu.add(menu_item_assign_region_group(region));
        }

        region_menu.on_natural_close(player2 -> get_module().menus.main_menu.create(player2).open(player2));

        return region_menu;
    }

    private MenuWidget menu_item_rename(final Region region) {
        return new MenuItem(0, item_rename.item(), (player, menu, self) -> {
            menu.close(player);
            if (!get_module().may_administrate(player, region)) {
                return ClickResult.ERROR;
            }

            get_module()
                .menus.enter_region_name_menu.create(player, region.name(), (player2, name) -> {
                    region.name(name);

                    // Update map marker
                    get_module().update_marker(region);

                    // Open new menu because of possibly changed title
                    get_module().menus.region_menu.create(region, player2).open(player2);
                    return ClickResult.SUCCESS;
                })
                .on_natural_close(player2 -> {
                    // Open new menu because of possibly changed title
                    get_module().menus.region_menu.create(region, player2).open(player2);
                })
                .open(player);

            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menu_item_delete(final Region region) {
        return new MenuItem(1, item_delete.item(), (player, menu, self) -> {
            menu.close(player);
            MenuFactory.confirm(
                get_context(),
                lang_delete_confirm_title.str(),
                item_delete_confirm_accept.item(),
                player2 -> {
                    if (!get_module().may_administrate(player2, region)) {
                        return ClickResult.ERROR;
                    }

                    get_module().remove_region(region);

                    // Give back money
                    final var temp_sel = new RegionSelection(get_module());
                    temp_sel.primary = region.extent().min();
                    temp_sel.secondary = region.extent().max();

                    final var price = temp_sel.price();
                    if (get_module().config_economy_as_currency) {
                        final var transaction = get_module().economy.deposit(player2, price);
                        if (!transaction.transactionSuccess()) {
                            get_module()
                                .log.severe(
                                    "Player " +
                                    player2 +
                                    " deleted region '" +
                                    region.name() +
                                    "' (cost " +
                                    price +
                                    ") but the economy plugin failed to deposit:"
                                );
                            get_module().log.severe("Error message: " + transaction.errorMessage);
                        }
                    } else {
                        give_items(player2, new ItemStack(get_module().config_currency), (int) price);
                    }
                    return ClickResult.SUCCESS;
                },
                item_delete_confirm_cancel.item(),
                player2 -> menu.open(player2)
            )
                .tag(new RegionMenuTag(region.id()))
                .open(player);
            return ClickResult.SUCCESS;
        });
    }

    private MenuWidget menu_item_assign_region_group(final Region region) {
        return new MenuItem(2, item_assign_region_group.item(), (player, menu, self) -> {
            menu.close(player);
            final var all_region_groups = get_module()
                .all_region_groups()
                .stream()
                .filter(g -> get_module().may_administrate(player, g))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toList());

            final var filter = new Filter.StringFilter<RegionGroup>((r, str) -> r.name().toLowerCase().contains(str));
            MenuFactory.generic_selector(
                get_context(),
                player,
                lang_select_region_group_title.str(),
                lang_filter_region_groups_title.str(),
                all_region_groups,
                r -> item_select_region_group.item("§a§l" + r.name()),
                filter,
                (player2, m, group) -> {
                    if (!get_module().may_administrate(player2, region)) {
                        return ClickResult.ERROR;
                    }

                    m.close(player2);
                    region.region_group_id(group.id());
                    mark_persistent_storage_dirty();
                    menu.open(player2);
                    return ClickResult.SUCCESS;
                },
                player2 -> menu.open(player2)
            ).open(player);
            return ClickResult.SUCCESS;
        });
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
