package org.oddlama.vane.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.menu.Menu.ClickResult;

public class MenuItemClickListener implements MenuWidget {

    private int slot;
    private Function4<Player, Menu, ItemStack, InventoryClickEvent, ClickResult> on_click;

    public MenuItemClickListener(int slot, final Function3<Player, Menu, ItemStack, ClickResult> on_click) {
        this(slot, (player, menu, item, event) -> {
            if (!Menu.is_left_click(event)) {
                return ClickResult.INVALID_CLICK;
            }
            return on_click.apply(player, menu, item);
        });
    }

    public MenuItemClickListener(
        int slot,
        final Function4<Player, Menu, ItemStack, InventoryClickEvent, ClickResult> on_click
    ) {
        this.slot = slot;
        this.on_click = on_click;
    }

    public int slot() {
        return slot;
    }

    public boolean update(final Menu menu) {
        return false;
    }

    public ClickResult click(
        final Player player,
        final Menu menu,
        final ItemStack item,
        int slot,
        final InventoryClickEvent event
    ) {
        if (this.slot != slot) {
            return ClickResult.IGNORE;
        }

        if (on_click != null) {
            return on_click.apply(player, menu, item, event);
        } else {
            return ClickResult.IGNORE;
        }
    }
}
