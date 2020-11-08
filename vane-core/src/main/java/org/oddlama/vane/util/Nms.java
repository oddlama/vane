package org.oddlama.vane.util;

import java.util.Map;
import java.util.logging.Level;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Clearable;
import net.minecraft.server.v1_16_R3.CreativeModeTab;
import net.minecraft.server.v1_16_R3.DataConverterRegistry;
import net.minecraft.server.v1_16_R3.DataConverterTypes;
import net.minecraft.server.v1_16_R3.DedicatedServer;
import net.minecraft.server.v1_16_R3.Enchantment;
import net.minecraft.server.v1_16_R3.EnchantmentSlotType;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.Item;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.SharedConstants;
import net.minecraft.server.v1_16_R3.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;
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
		if (item_stack == null) {
			return null;
		}

		if (!(item_stack instanceof CraftItemStack)) {
			return CraftItemStack.asNMSCopy(item_stack);
		}

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

	@SuppressWarnings("unchecked")
	public static void register_entity(final NamespacedKey base_entity_type, final String pseudo_namespace, final String key, final EntityTypes.Builder<?> builder) {
		final var s = pseudo_namespace + "_" + key;
		// From: https://papermc.io/forums/t/register-and-spawn-a-custom-entity-on-1-13-x/293
		// Get the datafixer
		final var world_version = SharedConstants.getGameVersion().getWorldVersion();
		final var world_version_key = DataFixUtils.makeKey(world_version);
		final var data_types = DataConverterRegistry.a()
			.getSchema(world_version_key)
			.findChoiceType(DataConverterTypes.ENTITY)
			.types();
		final var data_types_map = (Map<Object,Type<?>>)data_types;
		// Inject the new custom entity (this registers the key/id with the server,
		// so it will be available in vanilla constructs like the /summon command)
		data_types_map.put("minecraft:" + s, data_types_map.get(base_entity_type.toString()));
		// Store new type in registry
		IRegistry.a(IRegistry.ENTITY_TYPE, s, builder.a(s));
	}

	public static void spawn(org.bukkit.World world, Entity entity) {
		world_handle(world).addEntity(entity);
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
