package org.oddlama.vane.trifles.items.storage;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.EnumSet;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "backpack", base = Material.SHULKER_BOX, model_data = 0x760017, version = 1)
public class Backpack extends CustomItem<Trifles> {

    private static final String openedText = "§fOpened";

    public Backpack(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new SmithingRecipeDefinition("from_shulker_box")
                .base(Tag.SHULKER_BOXES)
                .addition(Material.LEATHER_CHESTPLATE)
                .copy_nbt(true)
                .result(key().toString())
        );
    }

    @EventHandler
    public void on_player_click_in_inventory(InventoryClickEvent event) {
        final var player = event.getWhoClicked();
        final var item = event.getCurrentItem();
        final var custom_item = get_module().core.item_registry().get(item);

        if (custom_item instanceof Backpack && item.getItemMeta().getLore().contains(openedText)) {
            player.setItemOnCursor(ItemStack.empty());
            event.setCancelled(true);
            player.sendActionBar(Component.text("You can't move opened backpacks."));
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

        ItemMeta backpackMeta = item.getItemMeta();

        if (custom_item instanceof Backpack) {
            backpackMeta.setLore(List.of());
            item.setItemMeta(backpackMeta);
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
        if (!(custom_item instanceof Backpack backpack) || !backpack.enabled()) {
            return;
        }

        // Never use anything else (e.g., offhand)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        ItemMeta backpackMeta = item.getItemMeta();

        if (get_module().storage_group.open_block_state_inventory(player, item)) {
            player.getWorld().playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f);
            swing_arm(player, event.getHand());
            backpackMeta.setLore(List.of(openedText));
            item.setItemMeta(backpackMeta);
        }
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.DISPENSE);
    }
}
