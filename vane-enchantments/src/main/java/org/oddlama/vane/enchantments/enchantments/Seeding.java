package org.oddlama.vane.enchantments.enchantments;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.MaterialUtil.seed_for;
import static org.oddlama.vane.util.MaterialUtil.farmland_for;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.is_replaceable_grass;
import static org.oddlama.vane.util.MaterialUtil.is_tillable;
import org.oddlama.vane.util.BlockUtil;
import static org.oddlama.vane.util.PlayerUtil.till_block;
import static org.oddlama.vane.util.PlayerUtil.seed_block;
import static org.oddlama.vane.util.BlockUtil.next_seedable_block;

@VaneEnchantment(name = "seeding", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Seeding extends CustomEnchantment<Enchantments> {
	public Seeding(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click_plant(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only seed when right clicking a plant
		final var plant_type = event.getClickedBlock().getType();
		if (!is_seeded_plant(plant_type)) {
			return;
		}

		// Get enchantment level
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItemInMainHand();
		final var level = item.getEnchantmentLevel(this.bukkit());
		if (level == 0) {
			return;
		}

		// Get seedable block
		final var seed_type = seed_for(plant_type);
		final var farmland_type = farmland_for(seed_type);
		final var seedable = next_seedable_block(event.getClickedBlock(), farmland_type, level);
		if (seedable == null) {
			return;
		}

		// Seed block
		if (seed_block(player, seedable, plant_type, seed_type)) {
			damage_item(player, item, 1);
		}
	}
}
