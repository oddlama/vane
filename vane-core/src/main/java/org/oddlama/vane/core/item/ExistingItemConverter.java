package org.oddlama.vane.core.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.module.Context;

public class ExistingItemConverter extends Listener<Core> {
    public ExistingItemConverter(final Context<Core> context) {
        super(context.namespace("existing_item_converter"));
    }

    private CustomItem from_old_item(final ItemStack item_stack) {
        final var modelDataList = item_stack.getItemMeta().getCustomModelDataComponent().getFloats();
        if (modelDataList.isEmpty() || modelDataList.getFirst() == null) {
            return null;
        }

        // If lookups fail, we return null and nothing will be done.
        String new_item_key = switch (modelDataList.getFirst().intValue()) {
            case 7758190 -> "vane_trifles:wooden_sickle";
            case 7758191 -> "vane_trifles:stone_sickle";
            case 7758192 -> "vane_trifles:iron_sickle";
            case 7758193 -> "vane_trifles:golden_sickle";
            case 7758194 -> "vane_trifles:diamond_sickle";
            case 7758195 -> "vane_trifles:netherite_sickle";
            case 7758254,7758255,7758256,7758257,7758258,7758259 -> "vane_trifles:file";
            case 7758318 -> "vane_trifles:empty_xp_bottle";
            case 7758382 -> "vane_trifles:small_xp_bottle";
            case 7758383 -> "vane_trifles:medium_xp_bottle";
            case 7758384 -> "vane_trifles:large_xp_bottle";
            case 7758446 -> "vane_trifles:home_scroll";
            case 7758510 -> "vane_trifles:unstable_scroll";
            case 7758574 -> "vane_trifles:reinforced_elytra";
            case 7823726 -> "vane_enchantments:ancient_tome";
            case 7823727 -> "vane_enchantments:enchanted_ancient_tome";
            case 7823790 -> "vane_enchantments:ancient_tome_of_knowledge";
            case 7823791 -> "vane_enchantments:enchanted_ancient_tome_of_knowledge";
            case 7823854 -> "vane_enchantments:ancient_tome_of_the_gods";
            case 7823855 -> "vane_enchantments:enchanted_ancient_tome_of_the_gods";
            default -> null;
        };

        if (new_item_key == null) {
            return null;
        }
        return get_module().item_registry().get(NamespacedKey.fromString(new_item_key));
    }

    private void process_inventory(@NotNull Inventory inventory) {
        final var contents = inventory.getContents();
        int changed = 0;

        for (int i = 0; i < contents.length; ++i) {
            final var is = contents[i];
            if (is == null || !is.hasItemMeta()) {
                continue;
            }

            final var custom_item = get_module().item_registry().get(is);
            if (custom_item == null) {
                // Determine if the item stack should be converted to a custom item from a legacy
                // definition
                final var convert_to_custom_item = from_old_item(is);
                if (convert_to_custom_item == null) {
                    continue;
                }

                contents[i] = convert_to_custom_item.convertExistingStack(is);
                contents[i].editMeta(meta -> meta.itemName(convert_to_custom_item.displayName()));
                get_module().enchantment_manager.update_enchanted_item(contents[i]);
                get_module().log.info("Converted legacy item to " + convert_to_custom_item.key());
                ++changed;
                continue;
            }

            // Remove obsolete custom items
            if (get_module().item_registry().shouldRemove(custom_item.key())) {
                contents[i] = null;
                get_module().log.info("Removed obsolete item " + custom_item.key());
                ++changed;
                continue;
            }

            // Update custom items to a new version, or if another detectable property changed.
            final var key_and_version = CustomItemHelper.customItemTagsFromItemStack(is);
            final var meta = is.getItemMeta();
            final var modelDataList = meta.getCustomModelDataComponent().getFloats();
            final Integer modelDataInt = (!modelDataList.isEmpty() && modelDataList.getFirst() != null)
                ? modelDataList.getFirst().intValue()
                : null;

            if (
                modelDataInt == null ||
                modelDataInt != custom_item.customModelData() ||
                is.getType() != custom_item.baseMaterial() ||
                key_and_version.getRight() != custom_item.version()) {
                // Also includes durability max update.
                contents[i] = custom_item.convertExistingStack(is);
                get_module().log.info("Updated item " + custom_item.key());
                ++changed;
                continue;
            }

            // Update maximum durability on existing items if changed.
            Damageable damageableMeta = (Damageable) contents[i].getItemMeta();
            int max_damage = damageableMeta.hasMaxDamage()
                ? damageableMeta.getMaxDamage()
                : contents[i].getType().getMaxDurability();
            int correct_max_damage = custom_item.durability() == 0
                ? contents[i].getType().getMaxDurability()
                : custom_item.durability();
            if (
                max_damage != correct_max_damage ||
                meta.getPersistentDataContainer().has(DurabilityManager.ITEM_DURABILITY_DAMAGE)) {
                get_module().log.info("Updated item durability " + custom_item.key());
                DurabilityManager.update_damage(custom_item, contents[i]);
                ++changed;
                continue;
            }
        }

        if (changed > 0) {
            inventory.setContents(contents);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_join(final PlayerJoinEvent event) {
        process_inventory(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_inventory_open(final InventoryOpenEvent event) {
        // Catches enderchests, and inventories by other plugins
        process_inventory(event.getInventory());
    }
}
