package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.MaterialUtil.is_tillable;
import static org.oddlama.vane.util.Nms.item_handle;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.SmithingRecipe;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.item.api.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;

// TODO recipe book click event
public class VanillaFunctionalityInhibitor extends Listener<Core> {
	public VanillaFunctionalityInhibitor(Context<Core> context) {
		super(context);
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
		if (!(recipe instanceof Keyed keyed)) {
			return;
		}

		// Only consider canceling minecraft's recipes
		if (!keyed.getKey().getNamespace().equals("minecraft")) {
			return;
		}

		for (final var item : event.getInventory().getMatrix()) {
			if (inhibit(get_module().item_registry().get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
				event.getInventory().setResult(null);
				return;
			}
		}
	}

	// Prevent custom items from being used in smithing by default. They have to override this event to allow it.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_smithing(final PrepareSmithingEvent event) {
		final var item = event.getInventory().getInputEquipment();
		final var recipe = event.getInventory().getRecipe();
		if (!(recipe instanceof Keyed keyed)) {
			return;
		}

		// Only consider canceling minecraft's recipes
		if (!keyed.getKey().getNamespace().equals("minecraft")) {
			return;
		}

		if (inhibit(get_module().item_registry().get(item), InhibitBehavior.USE_IN_VANILLA_RECIPE)) {
			event.getInventory().setResult(null);
		}
	}

	// If the result of a smithing recipe is a custom item, copy and merge input NBT data.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_prepare_smithing_copy_nbt(final PrepareSmithingEvent event) {
		var result = event.getResult();
		final var recipe = event.getInventory().getRecipe();
		if (result == null || !(recipe instanceof SmithingRecipe smithing_recipe) || !smithing_recipe.willCopyNbt()) {
			return;
		}

        // Actually use a recipe result, as copynbt has already modified the result
		result = recipe.getResult();
		final var custom_item_result = get_module().item_registry().get(result);
		if (custom_item_result == null) {
			return;
		}

		final var input = event.getInventory().getInputEquipment();
		final var input_components = CraftItemStack.asNMSCopy(input).getComponents();
		final var nms_result = CraftItemStack.asNMSCopy(result);
		nms_result.applyComponents(input_components);

		event.setResult(custom_item_result.convertExistingStack(CraftItemStack.asCraftMirror(nms_result)));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_anvil(final PrepareAnvilEvent event) {
		final var a = event.getInventory().getFirstItem();
		final var b = event.getInventory().getSecondItem();

		// Always prevent custom item repair with the custom item base material
		// if it is not also a matching custom item.
		// TODO: what about inventory based item repair?
		if (a != null && b != null && a.getType() == b.getType()) {
			// Disable the result unless a and b are instances of the same custom item.
			final var custom_item_a = get_module().item_registry().get(a);
			final var custom_item_b = get_module().item_registry().get(b);
			if (custom_item_a != null && custom_item_a != custom_item_b) {
				event.setResult(null);
				return;
			}
		}

		final var r = event.getInventory().getResult();
		if (r != null) {
			final var custom_item_r = get_module().item_registry().get(r);
			final boolean[] did_edit = new boolean[]{ true };
			r.editMeta(meta -> {
				if (a != null && inhibit(custom_item_r, InhibitBehavior.NEW_ENCHANTS)) {
					for (final var ench : r.getEnchantments().keySet()) {
						if (!a.getEnchantments().containsKey(ench)) {
							meta.removeEnchant(ench);
							did_edit[0] = true;
						}
					}
				}

				if (inhibit(custom_item_r, InhibitBehavior.MEND)) {
					meta.removeEnchant(Enchantment.MENDING);
					did_edit[0] = true;
				}
			});

			if (did_edit[0]) {
				event.setResult(r);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_item_mend(final PlayerItemMendEvent event) {
		final var item = event.getItem();
		final var custom_item = get_module().item_registry().get(item);

		// No repairing for mending inhibited items.
		if (inhibit(custom_item, InhibitBehavior.MEND)) {
			event.setCancelled(true);
		}
	}

	// Prevent netherite items from burning, as they are made of netherite
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_item_burn(final EntityDamageEvent event) {
		// Only burn damage on dropped items
		if (event.getEntity().getType() != EntityType.ITEM) {
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

	// Deny off-hand usage if the main hand is a custom item that inhibits off-hand usage.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.OFF_HAND) {
			return;
		}

		final var player = event.getPlayer();
		final var main_item = player.getEquipment().getItem(EquipmentSlot.HAND);
		final var main_custom_item = get_module().item_registry().get(main_item);
		if (inhibit(main_custom_item, InhibitBehavior.USE_OFFHAND)) {
			event.setUseItemInHand(Event.Result.DENY);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_dispense(BlockDispenseEvent event) {
		if(event.getBlock().getType() != Material.DISPENSER) {
			return;
		}

		final var custom_item = get_module().item_registry().get(event.getItem());
		if(inhibit(custom_item, InhibitBehavior.DISPENSE)) {
			event.setCancelled(true);
		}
	}
}
