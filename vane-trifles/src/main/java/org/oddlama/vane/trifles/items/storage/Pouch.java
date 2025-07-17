package org.oddlama.vane.trifles.items.storage;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.EnumSet;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "pouch", base = Material.DROPPER, model_data = 0x760016, version = 1)
public class Pouch extends CustomItem<Trifles> {

    private static final String openedText = "§fOpened";
    private final NamespacedKey openedKey;


    public Pouch(Context<Trifles> context) {
        super(context);
        this.openedKey = new NamespacedKey(Trifles.getPlugin(Trifles.class), "opened");
    }

    @Override
    public @NotNull ItemStack updateItemStack(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(openedKey, PersistentDataType.BOOLEAN, false);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("sls", "l l", "lll")
                .set_ingredient('s', Material.STRING)
                .set_ingredient('l', Material.RABBIT_HIDE)
                .result(key().toString())
        );
    }

    @EventHandler
    public void on_player_pickup_item(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        final var custom_item = get_module().core.item_registry().get(itemStack);
        if (custom_item instanceof Pouch) {
            boolean opened = itemStack.getItemMeta().getPersistentDataContainer().get(openedKey, PersistentDataType.BOOLEAN);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (opened) {
                itemMeta.getPersistentDataContainer().set(openedKey, PersistentDataType.BOOLEAN, false);
                itemStack.setItemMeta(itemMeta);
                item.setItemStack(itemStack);
            }
        }

    }

    @EventHandler
    public void on_player_click_in_inventory(InventoryClickEvent event) {
        final var player = event.getWhoClicked();
        final var item = event.getCurrentItem();
        final var custom_item = get_module().core.item_registry().get(item);

        // If the clicked item is a pouch and it has the "Opened" tooltip, cancel the click event.
        boolean opened = item.getItemMeta().getPersistentDataContainer().get(openedKey, PersistentDataType.BOOLEAN);

        if (custom_item instanceof Pouch && opened) {
            player.setItemOnCursor(ItemStack.empty());
            event.setCancelled(true);
            player.sendActionBar(Component.text("You can't move opened pouches."));
        }
        }

    @EventHandler
    public void on_player_close_inventory(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        final var item = player.getEquipment().getItem(event.getPlayer().getActiveItemHand());
        final var custom_item = get_module().core.item_registry().get(item);

        ItemMeta pouchMeta = item.getItemMeta();

        if (custom_item instanceof Pouch) {
            pouchMeta.setLore(List.of());
            pouchMeta.getPersistentDataContainer().set(openedKey, PersistentDataType.BOOLEAN, false);
            item.setItemMeta(pouchMeta);
        }
    }

    // ignoreCancelled = false to catch right-click-air events
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void on_player_right_click(final PlayerInteractEvent event) {
        if (!event.hasItem() || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Any right click to open
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Assert this is a matching custom item
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof Pouch pouch) || !pouch.enabled()) {
            return;
        }

        // Never use anything else (e.g., offhand)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        ItemMeta pouchMeta = item.getItemMeta();

        if (get_module().storage_group.open_block_state_inventory(player, item)) {
            player.getWorld().playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f);
            swing_arm(player, event.getHand());
            pouchMeta.setLore(List.of(openedText));
            pouchMeta.getPersistentDataContainer().set(openedKey, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(pouchMeta);
        }
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE);
    }
}
