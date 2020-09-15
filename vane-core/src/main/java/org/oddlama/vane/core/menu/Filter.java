package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.Nms.player_handle;

import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.ChatMessage;
import net.minecraft.server.v1_16_R2.ContainerAccess;
import net.minecraft.server.v1_16_R2.ContainerAnvil;
import net.minecraft.server.v1_16_R2.EntityHuman;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.PacketPlayOutOpenWindow;

import org.bukkit.entity.Player;

import java.util.List;
import org.oddlama.vane.core.module.Context;

public interface Filter<T> {
	public void open_filter_settings(final Player player, final Menu return_to);
	public List<T> filter(final List<T> things);
}
