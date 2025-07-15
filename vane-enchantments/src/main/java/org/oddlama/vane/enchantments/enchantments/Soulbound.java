package org.oddlama.vane.enchantments.enchantments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.data.CooldownData;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.util.StorageUtil;

@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allow_custom = true)
public class Soulbound extends CustomEnchantment<Enchantments> {

    @ConfigLong(
        def = 2000,
        min = 0,
        desc = "Window to allow Soulbound item drop immediately after a previous drop in milliseconds"
    )
    public long config_cooldown;

    private static final NamespacedKey IGNORE_SOULBOUND_DROP = StorageUtil.namespaced_key(
        "vane_enchantments",
        "ignore_soulbound_drop"
    );
    private CooldownData drop_cooldown = new CooldownData(IGNORE_SOULBOUND_DROP, config_cooldown);

    @LangMessage
    public TranslatedMessage lang_drop_lock_warning;

    @LangMessage
    public TranslatedMessage lang_dropped_notification;

    @LangMessage
    public TranslatedMessage lang_drop_cooldown;

    public Soulbound(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public void on_config_change() {
        super.on_config_change();
        drop_cooldown = new CooldownData(IGNORE_SOULBOUND_DROP, config_cooldown);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("cqc", "obe", "rgt")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_the_gods")
                .set_ingredient('c', Material.CHAIN)
                .set_ingredient('q', Material.WRITABLE_BOOK)
                .set_ingredient('o', Material.BONE)
                .set_ingredient('r', "minecraft:enchanted_book#enchants{minecraft:binding_curse*1}")
                .set_ingredient('g', Material.GHAST_TEAR)
                .set_ingredient('t', Material.TOTEM_OF_UNDYING)
                .set_ingredient('e', Material.ENDER_EYE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public LootTableList default_loot_tables() {
        return LootTableList.of(
            new LootDefinition("generic")
                .in(LootTables.BASTION_TREASURE)
                .add(1.0 / 15, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public Component apply_display_format(Component component) {
        return component.color(NamedTextColor.DARK_GRAY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_death(final PlayerDeathEvent event) {
        final var keep_items = event.getItemsToKeep();

        // Keep all soulbound items
        final var it = event.getDrops().iterator();
        while (it.hasNext()) {
            final var drop = it.next();
            if (is_soulbound(drop)) {
                keep_items.add(drop);
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_player_inventory_check(final InventoryClickEvent event) {
        if (event.getCursor() == null) return;
        if (!is_soulbound(event.getCursor())) return;
        if (
            event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR
        ) {
            boolean too_slow = drop_cooldown.peek_cooldown(event.getCursor().getItemMeta());
            if (too_slow) {
                // Dropped too slowly, refresh and cancel
                final ItemMeta meta = event.getCursor().getItemMeta();
                drop_cooldown.check_or_update_cooldown(meta);
                event.getCursor().setItemMeta(meta);
                lang_drop_cooldown.send_action_bar(event.getWhoClicked());
                event.setResult(Event.Result.DENY);
                return;
            }
            // else allow as normal
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_player_drop_item(final PlayerDropItemEvent event) {
        // A player cannot drop soulbound items.
        // Prevents yeeting your best sword out of existence.
        // (It's okay to put them into chests.)
        final var dropped_item = event.getItemDrop().getItemStack();
        if (is_soulbound(dropped_item)) {
            boolean too_slow = drop_cooldown.peek_cooldown(dropped_item.getItemMeta());
            if (!too_slow) {
                var meta = dropped_item.getItemMeta();
                drop_cooldown.clear(dropped_item.getItemMeta());
                dropped_item.setItemMeta(meta);
                lang_dropped_notification.send(event.getPlayer(), dropped_item.displayName());
                return;
            }
            final var inventory = event.getPlayer().getInventory();
            if (inventory.firstEmpty() != -1) {
                // We still have space in the inventory, so the player tried to drop it with Q.
                event.setCancelled(true);
                lang_drop_lock_warning.send_action_bar(
                    event.getPlayer(),
                    event.getItemDrop().getItemStack().displayName()
                );
            } else {
                // Inventory is full (e.g., when exiting crafting table with soulbound item in it)
                // so we drop the first non-soulbound item (if any) instead.
                final var it = inventory.iterator();
                ItemStack non_soulbound_item = null;
                int non_soulbound_item_slot = 0;
                while (it.hasNext()) {
                    final var item = it.next();
                    if (item.getEnchantmentLevel(this.bukkit()) == 0) {
                        non_soulbound_item = item;
                        break;
                    }

                    ++non_soulbound_item_slot;
                }

                if (non_soulbound_item == null) {
                    // We can't prevent dropping a soulbound item.
                    // Well, that sucks.
                    return;
                }

                // Drop the other item
                final var player = event.getPlayer();
                inventory.setItem(non_soulbound_item_slot, dropped_item);
                player.getLocation().getWorld().dropItem(player.getLocation(), non_soulbound_item);
                lang_drop_lock_warning.send_action_bar(player, event.getItemDrop().getItemStack().displayName());
                event.setCancelled(true);
            }
        }
    }

    private boolean is_soulbound(ItemStack dropped_item) {
        return dropped_item.getEnchantmentLevel(this.bukkit()) > 0;
    }
}
