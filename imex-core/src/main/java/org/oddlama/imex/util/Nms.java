package org.oddlama.imex.util;

import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import net.minecraft.server.v1_16_R1.EntityPlayer;

public class Nms {
	public static EntityPlayer getPlayer(Player player) {
		return ((CraftPlayer)player).getHandle();
	}
}
