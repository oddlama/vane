package org.oddlama.vane.enchantments;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.set.RegistryKeySet;
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

    TagKey<Enchantment> exclusive_with_tags;
    List<TypedKey<Enchantment>> exclusive_with = List.of();

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

    /**
     * Add exclusive enchantments to this enchantment: exclusive enchantments can't be on the
     * same tool.
     */
    public CustomEnchantmentRegistry exclusive_with(List<TypedKey<Enchantment>> enchantments){
        this.exclusive_with = enchantments;
        return this;
    }

    /**
     * Add exclusive enchantment <b>tag</b> to this enchantment: exclusive enchantments can't be on the
     * same tool.
     */
    public CustomEnchantmentRegistry exclusive_with(TagKey<Enchantment> enchantment_tag){
        this.exclusive_with_tags = enchantment_tag;
        return this;
    }

    /**
     * Get exclusive enchantments
     */
    public RegistryKeySet<Enchantment> exclusive_with(RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.Builder> freezeEvent) {
        if(this.exclusive_with_tags != null) {
            return freezeEvent.getOrCreateTag(exclusive_with_tags);
        } else {
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, this.exclusive_with);
        }
    }

    /**
     * Register the enchantment in the registry
     *
     * @see https://docs.papermc.io/paper/dev/registries#create-new-entries
     */
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
            .exclusiveWith(this.exclusive_with(freezeEvent))
            );
    }

    public TypedKey<Enchantment> typedKey(String name) {
        return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(NAMESPACE, name));
    }

}
