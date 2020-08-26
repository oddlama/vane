package org.oddlama.vane.util;

import org.bukkit.craftbukkit.v1_16_R1.util.CraftNamespacedKey;
import net.minecraft.server.v1_16_R1.EntityPlayer;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import net.minecraft.server.v1_16_R1.ItemStack;
import org.jetbrains.annotations.NotNull;
import net.minecraft.server.v1_16_R1.Enchantment;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

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
}
