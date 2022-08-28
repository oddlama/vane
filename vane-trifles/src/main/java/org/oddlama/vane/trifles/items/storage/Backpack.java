package org.oddlama.vane.trifles.items.storage;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.ArrayList;
import java.util.EnumSet;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeDefinition;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.MaterialUtil;

@VaneItem(name = "backpack", base = Material.SHULKER_BOX, model_data = 0x760017 /* until 0x760027, inclusive */, version = 1)
public class Backpack extends CustomItem<Trifles> {
	public Backpack(Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		final var list = new ArrayList<RecipeDefinition>();
		list.add(new SmithingRecipeDefinition("from_shulker_box")
			.base(Material.SHULKER_BOX)
			.addition(Material.NETHERITE_INGOT)
			.copy_nbt(true)
			.result(key().toString()));

		for (final var color : DyeColor.values()) {
			final var color_name = color.toString().toLowerCase();
			list.add(new SmithingRecipeDefinition("from_" + color_name + "_shulker_box")
				.base(MaterialUtil.material_from(NamespacedKey.minecraft(color_name + "_shulker_box")))
				.addition(Material.NETHERITE_INGOT)
				.copy_nbt(true)
				// TODO custom model data
				//.result(key().toString() + "[]"));
				.result(key().toString()));
			// TODO remove color how?
		}

		return new RecipeList(list);
	}

	// ignoreCancelled = false to catch right-click-air events
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (!event.hasItem() || event.useItemInHand() == Event.Result.DENY) {
			return;
		}

		// Any right click to open
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		// Assert this is a matching custom item
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		final var custom_item = get_module().core.item_registry().get(item);
		if (!(custom_item instanceof Backpack backpack) || !backpack.enabled()) {
			return;
		}

		// Never use anything else (e.g. offhand)
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);

		if (get_module().storage_group.open_block_state_inventory(player, item)) {
			player.getWorld().playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f);
			swing_arm(player, event.getHand());
		}
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE);
	}
}
