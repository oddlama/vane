package org.oddlama.vane.core.itemv2;

import static org.oddlama.vane.util.MaterialUtil.is_tillable;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.itemv2.api.CustomItem;
import org.oddlama.vane.core.itemv2.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;

// TODO recipe book click event
public class VanillaFunctionalityInhibitor extends Listener<Core> {
	public VanillaFunctionalityInhibitor(Context<Core> context) {
		super(context);
	}

	@Override
	protected void on_enable() {
	}

	@Override
	protected void on_disable() {
	}

	private boolean inhibit(CustomItem custom_item, InhibitBehavior behavior) {
		return custom_item != null && custom_item.enabled() && custom_item.inhibitedBehaviors().contains(behavior);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_pathfind(final EntityTargetEvent event) {
		if (event.getReason() != EntityTargetEvent.TargetReason.TEMPT) {
			return;
		}

		if (event.getTarget() instanceof Player player) {
			final var custom_item_main = get_module().item_registry().get(player.getInventory().getItemInMainHand());
			final var custom_item_off = get_module().item_registry().get(player.getInventory().getItemInOffHand());

			if (inhibit(custom_item_main, InhibitBehavior.TEMPT) || inhibit(custom_item_off, InhibitBehavior.TEMPT)) {
				event.setCancelled(true);
			}
		}
	}

	// Prevent custom hoe items from tilling blocks
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_hoe_right_click_block(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only when clicking a tillable block
		if (!is_tillable(event.getClickedBlock().getType())) {
			return;
		}

		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		if (inhibit(get_module().item_registry().get(item), InhibitBehavior.HOE_TILL)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_item_craft(final PrepareItemCraftEvent event) {
		final var recipe = event.getRecipe();
		if (recipe == null) {
			return;
		}

		final NamespacedKey key;
		if (recipe instanceof Keyed) {
			key = ((Keyed) recipe).getKey();
		} else {
			return;
		}

		// Only consider to cancel minecraft's recipes
		if (!key.getNamespace().equals("minecraft")) {
			return;
		}

		for (final var item : event.getInventory().getMatrix()) {
			if (inhibit(get_module().item_registry().get(item), InhibitBehavior.USE_IN_VANILLA_CRAFTING_RECIPE)) {
				event.getInventory().setResult(null);
				return;
			}
		}
	}

	// Prevent custom items from forming netherite variants, or delegate event to custom item
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_smithing(final PrepareSmithingEvent event) {
		final var item = event.getInventory().getInputEquipment();
		if (inhibit(get_module().item_registry().get(item), InhibitBehavior.USE_IN_SMITHING_RECIPE)) {
			event.getInventory().setResult(null);
		}
	}

	// TODO prevent repair with non-custom-item base material

	// Prevent netherite items from burning, as they are made of netherite
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_item_burn(final EntityDamageEvent event) {
		// Only burn damage on dropped items
		if (event.getEntity().getType() != EntityType.DROPPED_ITEM) {
			return;
		}

		switch (event.getCause()) {
			default:
				return;
			case FIRE:
			case FIRE_TICK:
			case LAVA:
				break;
		}

		final var entity = event.getEntity();
		if (!(entity instanceof Item)) {
			return;
		}

		final var item = ((Item)entity).getItemStack();
		if (inhibit(get_module().item_registry().get(item), InhibitBehavior.ITEM_BURN)) {
			event.setCancelled(true);
		}
	}
}
