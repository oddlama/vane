package org.oddlama.vane.core.menu;

import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class GenericSelector<T, F extends Filter<T>> {

    private MenuManager menu_manager;
    private Function1<T, ItemStack> to_item;
    private Function4<Player, Menu, T, InventoryClickEvent, ClickResult> on_click;

    private List<T> things;
    private F filter;
    private int page_size;

    private boolean update_filter = true;
    private int page = 0;
    private int last_page = 0;
    private List<T> filtered_things = null;

    private GenericSelector() {}

    public static <T, F extends Filter<T>> Menu create(
        final Context<?> context,
        final Player player,
        final String title,
        final String filter_title,
        final List<T> things,
        final Function1<T, ItemStack> to_item,
        final F filter,
        final Function4<Player, Menu, T, InventoryClickEvent, ClickResult> on_click,
        final Consumer1<Player> on_cancel
    ) {
        final var columns = 9;

        final var generic_selector = new GenericSelector<T, F>();
        generic_selector.menu_manager = context.get_module().core.menu_manager;
        generic_selector.to_item = to_item;
        generic_selector.on_click = on_click;
        generic_selector.things = things;
        generic_selector.filter = filter;
        generic_selector.page_size = 5 * columns;

        final var generic_selector_menu = new Menu(
            context,
            Bukkit.createInventory(null, 6 * columns, LegacyComponentSerializer.legacySection().deserialize(title))
        ) {
            @Override
            public void update(boolean force_update) {
                if (generic_selector.update_filter) {
                    // Filter list before update
                    generic_selector.filtered_things = generic_selector.filter.filter(generic_selector.things);
                    generic_selector.page = 0;
                    generic_selector.last_page =
                        Math.max(0, generic_selector.filtered_things.size() - 1) / generic_selector.page_size;
                    generic_selector.update_filter = false;
                }
                super.update(force_update);
            }
        };

        // Selection area
        generic_selector_menu.add(new SelectionArea<>(generic_selector, 0));

        // Page selector
        generic_selector_menu.add(
            new PageSelector<>(generic_selector, generic_selector.page_size + 1, generic_selector.page_size + 8)
        );

        // Filter item
        generic_selector_menu.add(
            new MenuItem(
                generic_selector.page_size,
                generic_selector.menu_manager.generic_selector_filter.item(),
                (p, menu, self, event) -> {
                    if (!Menu.is_left_or_right_click(event)) {
                        return ClickResult.INVALID_CLICK;
                    }

                    if (event.getClick() == ClickType.RIGHT) {
                        generic_selector.filter.reset();
                        generic_selector.update_filter = true;
                        menu.update();
                    } else {
                        menu.close(p);
                        generic_selector.filter.open_filter_settings(context, p, filter_title, menu);
                        generic_selector.update_filter = true;
                    }
                    return ClickResult.SUCCESS;
                }
            )
        );

        // Cancel item
        generic_selector_menu.add(
            new MenuItem(
                generic_selector.page_size + 8,
                generic_selector.menu_manager.generic_selector_cancel.item(),
                (p, menu, self) -> {
                    menu.close(p);
                    on_cancel.apply(player);
                    return ClickResult.SUCCESS;
                }
            )
        );

        // On natural close call cancel
        generic_selector_menu.on_natural_close(on_cancel);

        return generic_selector_menu;
    }

    public static class PageSelector<T, F extends Filter<T>> implements MenuWidget {

        private static final int BIG_JUMP_SIZE = 5;

        private final GenericSelector<T, F> generic_selector;
        private final int slot_from; // Inclusive
        private final int slot_to; // Exclusive

        // Shows page selector from [from, too)
        public PageSelector(final GenericSelector<T, F> generic_selector, int slot_from, int slot_to) {
            this.generic_selector = generic_selector;
            this.slot_from = slot_from;
            this.slot_to = slot_to;
            if (this.slot_to - this.slot_from < 3) {
                throw new IllegalArgumentException("PageSelector needs at least 3 assigned slots!");
            }
            if (((this.slot_to - this.slot_from) % 2) == 0) {
                throw new IllegalArgumentException("PageSelector needs an uneven number of assigned slots!");
            }
        }

        @Override
        public boolean update(final Menu menu) {
            for (int slot = slot_from; slot < slot_to; ++slot) {
                final var i = slot - slot_from;
                final var offset = button_offset(i);
                final var page = page_for_offset(offset);
                final var no_op = page == generic_selector.page;
                final var actual_offset = page - generic_selector.page;
                final ItemStack item;
                if (i == (slot_to - slot_from) / 2) {
                    // Current page indicator
                    item = generic_selector.menu_manager.generic_selector_current_page.item(
                        "§6" + (page + 1),
                        "§6" + (generic_selector.last_page + 1),
                        "§6" + generic_selector.filtered_things.size()
                    );
                } else if (no_op) {
                    item = null;
                } else {
                    item = generic_selector.menu_manager.generic_selector_page.item_amount(
                        Math.abs(actual_offset),
                        "§6" + (page + 1)
                    );
                }

                menu.inventory().setItem(slot, item);
            }
            return true;
        }

        private int button_offset(int i) {
            if (i <= 0) {
                // Go back up to BIG_JUMP_SIZE pages
                return -BIG_JUMP_SIZE;
            } else if (i >= (slot_to - slot_from) - 1) {
                // Go forward up to BIG_JUMP_SIZE pages
                return BIG_JUMP_SIZE;
            } else {
                final var base = (slot_to - slot_from) / 2;
                return i - base;
            }
        }

        private int page_for_offset(int offset) {
            int page = generic_selector.page + offset;
            if (page < 0) {
                page = 0;
            } else if (page > generic_selector.last_page) {
                page = generic_selector.last_page;
            }
            return page;
        }

        @Override
        public ClickResult click(
            final Player player,
            final Menu menu,
            final ItemStack item,
            int slot,
            final InventoryClickEvent event
        ) {
            if (slot < slot_from || slot >= slot_to) {
                return ClickResult.IGNORE;
            }

            if (menu.inventory().getItem(slot) == null) {
                return ClickResult.IGNORE;
            }

            if (!Menu.is_left_click(event)) {
                return ClickResult.INVALID_CLICK;
            }

            final var offset = button_offset(slot - slot_from);
            generic_selector.page = page_for_offset(offset);

            menu.update();
            return ClickResult.SUCCESS;
        }
    }

    public static class SelectionArea<T, F extends Filter<T>> implements MenuWidget {

        private final GenericSelector<T, F> generic_selector;
        private final int first_slot;

        public SelectionArea(final GenericSelector<T, F> generic_selector, final int first_slot) {
            this.generic_selector = generic_selector;
            this.first_slot = first_slot;
        }

        @Override
        public boolean update(final Menu menu) {
            for (int i = 0; i < generic_selector.page_size; ++i) {
                final var idx = generic_selector.page * generic_selector.page_size + i;
                if (idx >= generic_selector.filtered_things.size()) {
                    menu.inventory().setItem(first_slot + i, null);
                } else {
                    menu
                        .inventory()
                        .setItem(
                            first_slot + i,
                            generic_selector.to_item.apply(generic_selector.filtered_things.get(idx))
                        );
                }
            }
            return true;
        }

        @Override
        public ClickResult click(
            final Player player,
            final Menu menu,
            final ItemStack item,
            int slot,
            final InventoryClickEvent event
        ) {
            if (slot < first_slot || slot >= first_slot + generic_selector.page_size) {
                return ClickResult.IGNORE;
            }

            if (menu.inventory().getItem(slot) == null) {
                return ClickResult.IGNORE;
            }

            final var idx = generic_selector.page * generic_selector.page_size + (slot - first_slot);
            return generic_selector.on_click.apply(player, menu, generic_selector.filtered_things.get(idx), event);
        }
    }
}
