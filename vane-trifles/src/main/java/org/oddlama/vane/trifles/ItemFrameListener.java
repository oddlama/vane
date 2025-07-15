package org.oddlama.vane.trifles;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class ItemFrameListener extends Listener<Trifles> {

	public ItemFrameListener(Context<Trifles> context) {
		super(
			context.group(
				"invisible_item_frame",
				"Right clicking on an item frame with shears equipped will make it disappear."
			)
		);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_event(PlayerInteractEntityEvent event) {
		final var player = event.getPlayer();
		var entity = event.getRightClicked();
		boolean isHoldingShears = player.getInventory().getItemInMainHand().getType() == Material.SHEARS;
		boolean entityIsItemFrame = entity.getType() == EntityType.ITEM_FRAME;

		if (isHoldingShears && entityIsItemFrame) {
			event.setCancelled(true);
			entity.setInvisible(!entity.isInvisible());
		}
	}
}
