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

import org.oddlama.vane.core.module.Context;

public class AnvilMenu extends Menu {
	private EntityPlayer entity;
	private AnvilContainer container;
	private int container_id;
	private String title;

	public AnvilMenu(final Context<?> context, final Player player, final String title) {
		super(context);

		this.title = title;
		this.entity = player_handle(player);
		this.container_id = entity.nextContainerCounter();
		this.container = new AnvilContainer(container_id, entity);
		this.container.setTitle(new ChatMessage(title));
		this.inventory = container.getBukkitView().getTopInventory();
	}

	@Override
	public void open_window(final Player player) {
		if (player_handle(player) != entity) {
			manager.get_module().log.warning("AnvilMenu.open() was called with a player for whom this inventory wasn't created!");
		}

		entity.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container_id, container.getType(), new ChatMessage(title)));
		entity.activeContainer = container;
		entity.activeContainer.addSlotListener(entity);
	}

	private class AnvilContainer extends ContainerAnvil {
		public AnvilContainer(int window_id, final EntityHuman entity) {
			super(window_id, entity.inventory, ContainerAccess.at(entity.world, new BlockPosition(0, 0, 0)));
			this.checkReachable = false;
		}

		@Override
		public void e() {
			super.e();
			this.levelCost.set(0);
		}
	}
}
