package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer2;

public class LootTable {

    private Map<NamespacedKey, List<LootTableEntry>> possible_loot = new HashMap<>();

    public LootTable() {}

    public LootTable put(final NamespacedKey key, final LootTableEntry entry) {
        possible_loot.put(key, List.of(entry));
        return this;
    }

    public LootTable put(final NamespacedKey key, final List<LootTableEntry> entries) {
        possible_loot.put(key, entries);
        return this;
    }

    public LootTable remove(final NamespacedKey key) {
        possible_loot.remove(key);
        return this;
    }

    public Map<NamespacedKey, List<LootTableEntry>> possible_loot() {
        return possible_loot;
    }

    public List<LootTableEntry> flat_copy() {
        List<LootTableEntry> list = new ArrayList<>();
        possible_loot.values().forEach(list::addAll);
        return list;
    }

    public void generate_loot(final List<ItemStack> output, final Random random) {
        for (final var set : possible_loot.values()) {
            for (final var loot : set) {
                if (loot.evaluate_chance(random)) {
                    loot.add_sample(output, random);
                }
            }
        }
    }

    public ItemStack generate_override(final Random random) {
        double total_chance = 0;
        final double threshold = random.nextDouble();
        final List<ItemStack> result_container = new ArrayList<>(1);
        final var loot_list = flat_copy();
        Collections.shuffle(loot_list, random);
        for (final var loot : loot_list) {
            total_chance += loot.chance;
            if (total_chance > threshold) {
                loot.add_sample(result_container, random);
            }
            if (!result_container.isEmpty()) {
                return result_container.get(0);
            }
        }
        return null;
    }

    public static class LootTableEntry {

        public double chance;
        public Consumer2<List<ItemStack>, Random> generator;

        public LootTableEntry(int rarity_expected_chests, final ItemStack item) {
            this(1.0 / rarity_expected_chests, item.clone(), 1, 1);
        }

        public LootTableEntry(int rarity_expected_chests, final ItemStack item, int amount_min, int amount_max) {
            this(1.0 / rarity_expected_chests, item.clone(), amount_min, amount_max);
        }

        public LootTableEntry(double chance, final ItemStack item, int amount_min, int amount_max) {
            this(chance, (list, random) -> {
                final var i = item.clone();
                final var amount = random.nextInt(amount_max - amount_min + 1) + amount_min;
                if (amount < 1) {
                    return;
                }

                i.setAmount(amount);
                list.add(i);
            });
        }

        public LootTableEntry(double chance, Consumer2<List<ItemStack>, Random> generator) {
            this.chance = chance;
            this.generator = generator;
        }

        public void add_sample(final List<ItemStack> items, final Random random) {
            generator.apply(items, random);
        }

        public boolean evaluate_chance(Random random) {
            return random.nextDouble() < chance;
        }
    }
}
