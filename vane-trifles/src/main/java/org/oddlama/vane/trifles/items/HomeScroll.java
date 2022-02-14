package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.Util.ms_to_ticks;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItemv2;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.itemv2.CustomItem;
import org.oddlama.vane.core.itemv2.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

import net.kyori.adventure.text.Component;

// TODO scroll base class
@VaneItemv2(name = "home_scroll", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 25, model_data = 7758446, version = 1)
public class HomeScroll extends CustomItem<Trifles> {
	@ConfigInt(def = 10000, min = 0, desc = "Cooldown in milliseconds until another scroll can be used.")
	private int config_cooldown;

	@ConfigInt(def = 15000, min = 0, desc = "A cooldown in milliseconds that is applied when the player takes damage (prevents combat logging). Set to 0 to allow combat logging.")
	private int config_damage_cooldown;

	public HomeScroll(Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("pip", "cbe", "plp")
			// TODO BADDDDDDDDDDDDDDDDDDDDDDDDDDDD TEST REMOVEEEEEEEEEEEEEEEEE
			.set_ingredient('b', "minecraft:stick{Enchantments:[{id:knockback,lvl:1000}]}")
			.set_ingredient('p', Material.MAP)
			.set_ingredient('i', Tag.BEDS)
			.set_ingredient('c', Material.COMPASS)
			.set_ingredient('e', Material.ENDER_PEARL)
			.set_ingredient('l', Material.CLOCK)
			.result(key().toString()));
	}

	//@Override
	//public LootTableList default_loot_tables() {
	//	// TODO spawn scroll with 1 usage! possible with nbt nice.
	//}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		if (event.useItemInHand() == Event.Result.DENY) {
			return;
		}

		// Assert this is a matching custom item
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		if (!isInstance(item)) {
			// TODO enabled check when making a baseclass
			return;
		}

		// Never actually use the base item if it's custom!
		event.setUseItemInHand(Event.Result.DENY);

		switch (event.getAction()) {
			default:
				return;
			case RIGHT_CLICK_AIR:
				break;
			case RIGHT_CLICK_BLOCK:
				// Require non-cancelled state (so it won't trigger for block-actions like chests)
				// But allow if the clicked block can't be interacted with in the first place
				if (event.useInteractedBlock() != Event.Result.DENY) {
					final var block = event.getClickedBlock();
					if (block.getType().isInteractable()) {
						return;
					}
					event.setUseInteractedBlock(Event.Result.DENY);
				}
				break;
		}

		final var to_location = player.getBedSpawnLocation();
		final var to_potential_location = player.getPotentialBedLocation();
		if (to_location == null) {
			if (to_potential_location != null) {
				// "You have no home bed or charged respawn anchor, or it was obstructed"
				// The most cursed sentence in minecraft.
				player.sendActionBar(Component.translatable("block.minecraft.spawn.not_valid"));
			} else {
				// "Sleep in a bed to change your respawn point"
				player.sendActionBar(Component.translatable("advancements.adventure.sleep_in_bed.description"));
			}
			return;
		}

		// Check cooldown
		if (player.getCooldown(baseMaterial()) > 0) {
			return;
		}

		final var current_location = player.getLocation();
		if (get_module().teleport_from_scroll(player, current_location, to_location)) {
			// Set cooldown
			final var cooldown = ms_to_ticks(config_cooldown);
			player.setCooldown(baseMaterial(), (int) cooldown);

			// Damage item
			damage_item(player, item, 1);
			swing_arm(player, event.getHand());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_take_damage(final EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		final var player = (Player) event.getEntity();
		// TODO enabled check when making a baseclass

		// Don't decrease cooldown
		final var damage_cooldown = (int) ms_to_ticks(config_damage_cooldown);
		if (player.getCooldown(baseMaterial()) >= damage_cooldown) {
			return;
		}

		player.setCooldown(baseMaterial(), damage_cooldown);
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_CRAFTING_RECIPE, InhibitBehavior.USE_IN_SMITHING_RECIPE, InhibitBehavior.TEMPT);
	}
}
