package org.oddlama.vane.core.enchantments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Nms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.item.crafting.RecipeHolder;

public class CustomEnchantmentFixer extends Listener<Core> {

    ArrayList<PacketAdapter> containerAdapters = new ArrayList<PacketAdapter>();

    public CustomEnchantmentFixer(Context<Core> context) {
        super(context);
        
    }

    @Override
    protected void on_enable() {
        super.on_enable();
        for (var type : new PacketType[] {
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.SET_SLOT,
        }) {
            var a = new ContainerAdapter(type);
            get_module().protocol_manager.addPacketListener(a);
            containerAdapters.add(a);
        }

        var recipeAdapter = new RecipeAdapter();
        get_module().protocol_manager.addPacketListener(recipeAdapter);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreativeItemMove(InventoryCreativeEvent event) {
        Optional<Enchantment> vaneEnchantment = event.getCurrentItem().getEnchantments().keySet().stream()
            .filter((enchantment) -> enchantment.getKey().getNamespace().startsWith("vane"))
            .findFirst();
        if(vaneEnchantment.isPresent()) {
            event.setCancelled(true);
            event.getView().getPlayer().sendMessage(Component.text("Moving items with Vane enchantments in creative mode is temporarily inactive.", NamedTextColor.GOLD));
        }
    }

    public static ItemStack removeVaneEnchants(ItemStack item) {
        if (item == null || item.getType() == null || item.getType() == Material.AIR)
            return item;
        ItemStack itemCopy = item.clone();

        itemCopy.getEnchantments().forEach((enchantment, level) -> {
            if (enchantment.getKey().getNamespace().startsWith("vane")) {
                itemCopy.removeEnchantment(enchantment);
            }
        });

        // also remove stored enchantments for enchanted books
        if (itemCopy.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemCopy.getItemMeta();
            meta.getStoredEnchants().forEach((enchantment, level) -> {
                if (enchantment.getKey().getNamespace().startsWith("vane")) {
                    meta.removeStoredEnchant(enchantment);
                }
            });
            itemCopy.setItemMeta(meta);
        }

        return itemCopy;
    }

    class ContainerAdapter extends PacketAdapter {
        private PacketType type;

        public ContainerAdapter(PacketType type) {
            super(
                    CustomEnchantmentFixer.this.get_module(),
                    ListenerPriority.HIGHEST,
                    type);
            this.type = type;
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            if (event.getPacket().getHandle() instanceof ClientboundSetEntityDataPacket edp) {
                var newlist = edp.packedItems().stream().map(x -> {
                    if (x.value() instanceof net.minecraft.world.item.ItemStack itemStack && !itemStack.isEmpty()) {

                        var bukkititem = CustomEnchantmentFixer.removeVaneEnchants(itemStack.asBukkitCopy());
                        var newitem = Nms.item_handle(bukkititem);
                        return new DataValue<net.minecraft.world.item.ItemStack>(x.id(),
                                (EntityDataSerializer<net.minecraft.world.item.ItemStack>) x.serializer(), newitem);
                    }
                    return x;
                }).toList();
                var newPacket = new ClientboundSetEntityDataPacket(edp.id(), newlist);
                event.setPacket(PacketContainer.fromPacket(newPacket));
            }
            try {
                var mods = event.getPacket().getItemListModifier();
                for (int i = 0; i < mods.size(); ++i) {
                    try {
                        var slots = mods.readSafely(i);
                        List<ItemStack> newSlots = slots.stream()
                                .map((item) -> CustomEnchantmentFixer.removeVaneEnchants(item)).toList();
                        mods.writeSafely(i, newSlots);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
            try {
                var mods = event.getPacket().getItemArrayModifier();
                for (int i = 0; i < mods.size(); ++i) {
                    try {
                        var slots = mods.readSafely(i);
                        var newSlots = (ItemStack[]) Arrays.stream(slots)
                                .map((item) -> CustomEnchantmentFixer.removeVaneEnchants(item)).toArray();
                        mods.writeSafely(i, newSlots);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
            try {
                var mods = event.getPacket().getSlotStackPairLists();
                for (int i = 0; i < mods.size(); ++i) {
                    try {
                        var slots = mods.readSafely(i);
                        var newSlots = slots.stream()
                                .map((pair) -> new com.comphenix.protocol.wrappers.Pair<EnumWrappers.ItemSlot, ItemStack>(
                                        pair.getFirst(),
                                        CustomEnchantmentFixer.removeVaneEnchants(pair.getSecond())))
                                .toList();
                        mods.writeSafely(i, newSlots);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
            try {
                var mods = event.getPacket().getItemModifier();
                for (int i = 0; i < mods.size(); ++i) {
                    try {
                        var item = mods.readSafely(i);
                        item = CustomEnchantmentFixer.removeVaneEnchants(item);
                        mods.writeSafely(i, item);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    class RecipeAdapter extends PacketAdapter {
        public RecipeAdapter() {
            super(CustomEnchantmentFixer.this.get_module(),
            ListenerPriority.HIGHEST,
            PacketType.Play.Server.RECIPE_UPDATE);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            if(event.getPacketType().getCurrentId() != 0x77) {
                return;
            }
            var packet = (ClientboundUpdateRecipesPacket) event.getPacket().getHandle();
            
            var newRecipes = new ArrayList<RecipeHolder<?>>();
            for (RecipeHolder<?> recipe : packet.getRecipes()) {
                if(recipe.id().getNamespace().startsWith("vane_enchantment")) continue;
                newRecipes.add(recipe);
            }
                
            var newPacket = new PacketContainer(PacketType.Play.Server.RECIPE_UPDATE, new ClientboundUpdateRecipesPacket(newRecipes));
            
            event.setPacket(newPacket);            
        }
    }
    
}
