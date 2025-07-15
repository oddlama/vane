package org.oddlama.vane.core.enchantments;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public class EnchantmentManager extends Listener<Core> {

    private static final NamespacedKey SENTINEL = StorageUtil.namespaced_key("vane", "enchantment_lore");

    public EnchantmentManager(Context<Core> context) {
        super(context);
    }

    public ItemStack update_enchanted_item(ItemStack item_stack) {
        return update_enchanted_item(item_stack, new HashMap<Enchantment, Integer>(), false);
    }

    public ItemStack update_enchanted_item(ItemStack item_stack, Map<Enchantment, Integer> additional_enchantments) {
        return update_enchanted_item(item_stack, additional_enchantments, false);
    }

    public ItemStack update_enchanted_item(ItemStack item_stack, boolean only_if_enchanted) {
        return update_enchanted_item(item_stack, new HashMap<Enchantment, Integer>(), only_if_enchanted);
    }

    public ItemStack update_enchanted_item(
        ItemStack item_stack,
        Map<Enchantment, Integer> additional_enchantments,
        boolean only_if_enchanted
    ) {
        remove_old_lore(item_stack);
        return item_stack;
    }

    private void remove_superseded(ItemStack item_stack, Map<Enchantment, Integer> enchantments) {
        // if (enchantments.isEmpty()) {
        // 	return;
        // }

        // 1. Build a list of all enchantments that would be removed because
        //    they are superseded by some enchantment.
        // final var to_remove_inclusive = enchantments.keySet().stream()
        // 	.map(x -> ((CraftEnchantment)x).getHandle())
        // 	.filter(x -> x instanceof NativeEnchantmentWrapper)
        // 	.map(x -> ((NativeEnchantmentWrapper)x).custom().supersedes())
        // 	.flatMap(Set::stream)
        // 	.collect(Collectors.toSet());

        // 2. Before removing these enchantments, first re-build the list but
        //    ignore any enchantments in the calculation that would themselves
        //    be removed. This prevents them from contributing to the list of
        //    enchantments to remove. Consider this: A supersedes B, and B supersedes C, but
        //    A doesn't supersede C. Now an item with A B and C should get reduced to
        //    A and C, not just to A.
        // var to_remove = enchantments.keySet().stream()
        // 	.map(x -> ((CraftEnchantment)x).getHandle())
        // 	.filter(x -> x instanceof NativeEnchantmentWrapper)
        // 	.filter(x ->
        // !to_remove_inclusive.contains(((NativeEnchantmentWrapper)x).custom().key())) // Ignore
        // enchantments that are themselves removed.
        // 	.map(x -> ((NativeEnchantmentWrapper)x).custom().supersedes())
        // 	.flatMap(Set::stream)
        // 	.map(x -> org.bukkit.Registry.ENCHANTMENT.get(x))
        // 	.collect(Collectors.toSet());

        // for (var e : to_remove) {
        // 	item_stack.removeEnchantment(e);
        // 	enchantments.remove(e);
        // }
    }

    private void remove_old_lore(ItemStack item_stack) {
        var lore = item_stack.lore();
        if (lore == null) {
            lore = new ArrayList<Component>();
        }

        lore.removeIf(this::is_enchantment_lore);

        // Set lore
        item_stack.lore(lore.isEmpty() ? null : lore);
    }

    private boolean is_enchantment_lore(final Component component) {
        // FIXME legacy If the component begins with a translated lore from vane enchantments, it is
        // always from us. (needed for backward compatibility)
        if (
            component instanceof TranslatableComponent translatable_component &&
            translatable_component.key().startsWith("vane_enchantments.")
        ) {
            return true;
        }

        return ItemUtil.has_sentinel(component, SENTINEL);
    }

    // Triggers on Anvils, grindstones, and smithing tables.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_prepare_enchanted_edit(final PrepareResultEvent event) {
        if (event.getResult() == null) {
            return;
        }

        event.setResult(update_enchanted_item(event.getResult().clone()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_enchant_item(final EnchantItemEvent event) {
        final var map = new HashMap<Enchantment, Integer>(event.getEnchantsToAdd());
        update_enchanted_item(event.getItem(), map);
    }

    // @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // public void on_loot_generate(final LootGenerateEvent event) {
    // 	for (final var item : event.getLoot()) {
    // 		// Update all item lore in case they are enchanted
    // 		update_enchanted_item(item, true);
    // 	}
    // }

    private MerchantRecipe process_recipe(final MerchantRecipe recipe) {
        var result = recipe.getResult().clone();

        // Create a new recipe
        final var new_recipe = new MerchantRecipe(
            update_enchanted_item(result, true),
            recipe.getUses(),
            recipe.getMaxUses(),
            recipe.hasExperienceReward(),
            recipe.getVillagerExperience(),
            recipe.getPriceMultiplier()
        );
        recipe.getIngredients().forEach(i -> new_recipe.addIngredient(i));
        return new_recipe;
    }
    // @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    // public void on_acquire_trade(final VillagerAcquireTradeEvent event) {
    // 	event.setRecipe(process_recipe(event.getRecipe()));
    // }

    // @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    // public void on_right_click_villager(final PlayerInteractEntityEvent event) {
    // 	final var entity = event.getRightClicked();
    // 	if (!(entity instanceof Merchant)) {
    // 		return;
    // 	}

    // 	final var merchant = (Merchant) entity;
    // 	final var recipes = new ArrayList<MerchantRecipe>();

    // 	// Check all recipes
    // 	for (final var r : merchant.getRecipes()) {
    // 		recipes.add(process_recipe(r));
    // 	}

    // 	// Update recipes
    // 	merchant.setRecipes(recipes);
    // }
}
