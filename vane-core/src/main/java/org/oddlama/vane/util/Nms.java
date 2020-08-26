package org.oddlama.vane.util;

import net.minecraft.server.v1_16_R1.EntityPlayer;

import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import net.minecraft.server.v1_16_R1.Enchantment;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public class Nms {
	public static EntityPlayer get_player(Player player) {
		return ((CraftPlayer)player).getHandle();
	}

	public static void register_enchantment(NamespacedKey key, Enchantment enchantment) {
		IRegistry.a(IRegistry.ENCHANTMENT, new MinecraftKey(key.getNamespace(), key.getKey()), enchantment);
	}
}
