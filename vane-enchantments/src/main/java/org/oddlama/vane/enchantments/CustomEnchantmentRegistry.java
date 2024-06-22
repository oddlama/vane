package org.oddlama.vane.enchantments;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public abstract class CustomEnchantmentRegistry {

    final public static String NAMESPACE = "vane_enchantments";
    final private static String TRANSLATE_KEY = "vane_enchantments.enchantment_%s.name";
    Key key;
    Component description;
    int max_level;
    TagKey<ItemType> supported_item_tags;
    List<TypedKey<ItemType>> supported_items = List.of();

        
    public CustomEnchantmentRegistry(String name, TagKey<ItemType> supported_item_tags, int max_level) {
        this.key = Key.key(NAMESPACE, name);
        this.description = Component.translatable(String.format(TRANSLATE_KEY, name));
        this.supported_item_tags = supported_item_tags;
        this.max_level = max_level;
    }

    public CustomEnchantmentRegistry(String name, List<TypedKey<ItemType>> supported_items, int max_level) {
        this.key = Key.key(NAMESPACE, name);
        this.description = Component.translatable(String.format(TRANSLATE_KEY, name));
        this.supported_items = supported_items;
        this.max_level = max_level;
    }

    public void register(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent){
        freezeEvent.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, key),
        e -> e.description(description)
            .supportedItems(supported_items.size() > 0 ? RegistrySet.keySet(RegistryKey.ITEM, supported_items) : freezeEvent.getOrCreateTag(supported_item_tags))
            .anvilCost(1)
            .maxLevel(max_level)
            .weight(10)
            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
            .activeSlots(EquipmentSlotGroup.ANY)
            );
    }
}
