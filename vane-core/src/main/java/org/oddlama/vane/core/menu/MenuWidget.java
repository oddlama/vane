package org.oddlama.vane.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.menu.Menu.ClickResult;

public interface MenuWidget {
    public boolean update(final Menu menu);

    public ClickResult click(
        final Player player,
        final Menu menu,
        final ItemStack item,
        int slot,
        final InventoryClickEvent event
    );
}
