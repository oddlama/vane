package org.oddlama.vane.util;

import net.minecraft.server.v1_16_R1.EntityPlayer;

import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Nms {
	public static EntityPlayer getPlayer(Player player) {
		return ((CraftPlayer)player).getHandle();
	}
}
