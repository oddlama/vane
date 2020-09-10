package org.oddlama.vane.util;

import java.util.logging.Level;

import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.Clearable;
import net.minecraft.server.v1_16_R2.CreativeModeTab;
import net.minecraft.server.v1_16_R2.DedicatedServer;
import net.minecraft.server.v1_16_R2.Enchantment;
import net.minecraft.server.v1_16_R2.EnchantmentSlotType;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.Item;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class Nms {
	public static EntityPlayer get_player(Player player) {
		return ((CraftPlayer)player).getHandle();
	}

	public static void register_enchantment(NamespacedKey key, Enchantment enchantment) {
		IRegistry.a(IRegistry.ENCHANTMENT, new MinecraftKey(key.getNamespace(), key.getKey()), enchantment);
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

	public static WorldServer world_handle(org.bukkit.World world) {
		return ((CraftWorld)world).getHandle();
	}

	public static DedicatedServer server_handle() {
		final var bukkit_server = Bukkit.getServer();
		return ((CraftServer)bukkit_server).getServer();
	}

	public static int unlock_all_recipes(final org.bukkit.entity.Player player) {
		final var recipes = server_handle().getCraftingManager().b();
		return player_handle(player).discoverRecipes(recipes);
	}

	public static int creative_tab_id(final Item item) {
		CreativeModeTab tab = null;
		try {
			final var creative_mode_tab_field = Item.class.getDeclaredField("i");
			creative_mode_tab_field.setAccessible(true);
			tab = (CreativeModeTab)creative_mode_tab_field.get(item);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not get creative mode tab", e);
		}

		try {
			final var id_field = CreativeModeTab.class.getDeclaredField("o");
			id_field.setAccessible(true);
			return tab == null ? Integer.MAX_VALUE : (int)id_field.get(tab);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not get creative mode tab id", e);
			return Integer.MAX_VALUE;
		}
	}

	public static void set_air_no_drops(final org.bukkit.block.Block block) {
		final var tileentity = world_handle(block.getWorld()).getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ()));
		Clearable.a(tileentity);
		block.setType(Material.AIR, false);
	}
}
