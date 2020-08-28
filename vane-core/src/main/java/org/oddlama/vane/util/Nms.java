package org.oddlama.vane.util;

import java.util.List;
import java.util.logging.Level;

import net.minecraft.server.v1_16_R1.Enchantment;
import net.minecraft.server.v1_16_R1.EnchantmentSlotType;
import net.minecraft.server.v1_16_R1.EntityPlayer;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.ItemStack;
import net.minecraft.server.v1_16_R1.MinecraftKey;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import org.jetbrains.annotations.NotNull;

public class Nms {
	public static EntityPlayer get_player(Player player) {
		return ((CraftPlayer)player).getHandle();
	}

	public static void register_enchantment(NamespacedKey key, Enchantment enchantment) {
		IRegistry.a(IRegistry.ENCHANTMENT, new MinecraftKey(key.getNamespace(), key.getKey()), enchantment);
	}

	public static void set_lore(ItemMeta meta, List<IChatBaseComponent> lore) {
		if (!meta.getClass().getName().equals("org.bukkit.craftbukkit.v1_16_R1.inventory.CraftMetaItem")) {
			Bukkit.getLogger().warning("Called set_lore() on ItemMeta which isn't an instance of CraftMetaItem! Operation cancelled.");
			return;
		}

		try {
			final var lore_field = meta.getClass().getDeclaredField("lore");
			lore_field.setAccessible(true);
			lore_field.set(meta, lore);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			Bukkit.getLogger().log(Level.WARNING, "Could not set CraftMetaItem.lore field!", e);
		}
	}

	public static org.bukkit.enchantments.Enchantment bukkit_enchantment(Enchantment enchantment) {
		final var key = IRegistry.ENCHANTMENT.getKey(enchantment);
		return org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(key));
	}

	@NotNull
	public static org.bukkit.inventory.ItemStack bukkit_item_stack(ItemStack stack) {
		return CraftItemStack.asCraftMirror(stack);
	}

	public static EnchantmentSlotType enchantment_slot_type(EnchantmentTarget target) {
		switch (target) {
			case ARMOR:       return EnchantmentSlotType.ARMOR;
			case ARMOR_FEET:  return EnchantmentSlotType.ARMOR_FEET;
			case ARMOR_HEAD:  return EnchantmentSlotType.ARMOR_HEAD;
			case ARMOR_LEGS:  return EnchantmentSlotType.ARMOR_LEGS;
			case ARMOR_TORSO: return EnchantmentSlotType.ARMOR_CHEST;
			case TOOL:        return EnchantmentSlotType.DIGGER;
			case WEAPON:      return EnchantmentSlotType.WEAPON;
			case BOW:         return EnchantmentSlotType.BOW;
			case FISHING_ROD: return EnchantmentSlotType.FISHING_ROD;
			case BREAKABLE:   return EnchantmentSlotType.BREAKABLE;
			case WEARABLE:    return EnchantmentSlotType.WEARABLE;
			case TRIDENT:     return EnchantmentSlotType.TRIDENT;
			case CROSSBOW:    return EnchantmentSlotType.CROSSBOW;
			case VANISHABLE:  return EnchantmentSlotType.VANISHABLE;
			default:          return null;
		}
	}

	public static ItemStack item_handle(org.bukkit.inventory.ItemStack item_stack) {
		try {
			final var handle = CraftItemStack.class.getDeclaredField("handle");
			handle.setAccessible(true);
			return (ItemStack)handle.get(item_stack);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			return null;
		}
	}

	public static EntityPlayer player_handle(org.bukkit.entity.Player player) {
		if (!(player instanceof CraftPlayer)) {
			return null;
		}
		return ((CraftPlayer)player).getHandle();
	}
}
