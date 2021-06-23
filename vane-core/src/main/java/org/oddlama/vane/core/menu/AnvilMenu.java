package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.Nms.player_handle;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;

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
		if (tainted) {
			return;
		}

		if (player_handle(player) != entity) {
			manager.get_module().log.warning("AnvilMenu.open() was called with a player for whom this inventory wasn't created!");
		}

		entity.connection.sendPacket(new PacketPlayOutOpenWindow(container_id, container.getType(), new ChatMessage(title)));
		entity.initMenu(container);

		// This cast is necessary so the remapper understands that containerMenu is part of EntityHuman,
		// otherwise it doesn't recognize that this field needs to be renamed
		((EntityHuman)entity).containerMenu = container;
	}

	private class AnvilContainer extends ContainerAnvil {
		public AnvilContainer(int window_id, final EntityHuman entity) {
			super(window_id, entity.getInventory(), ContainerAccess.at(entity.getWorld(), new BlockPosition(0, 0, 0)));
			this.checkReachable = false;
		}

		@Override
		public void i() {
			super.i();
			this.cost.set(0);
		}

		@Override
		public void b(EntityHuman player) {
		}

		@Override
		protected void a(EntityHuman player, IInventory container) {
		}
	}
}
