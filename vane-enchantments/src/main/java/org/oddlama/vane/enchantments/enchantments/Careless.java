package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.is_replaceable_grass;
import static org.oddlama.vane.util.MaterialUtil.is_tillable;
import static org.oddlama.vane.util.PlayerUtil.till_block;

import com.destroystokyo.paper.MaterialTags;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.EventHandler;
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

@VaneEnchantment(name = "careless", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Careless extends CustomEnchantment<Enchantments> {
	public Careless(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click_replaceable(PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only when clicking a replaceable block
		if (!is_replaceable_grass(event.getClickedBlock().getType())) {
			return;
		}

		// Check if underlying block if tillable
		var below = event.getClickedBlock().getRelative(BlockFace.DOWN);
		if (below == null || !is_tillable(below.getType())) {
			return;
		}

		// Check enchantment
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItemInMainHand();
		if (item.getEnchantmentLevel(this.bukkit()) == 0) {
			return;
		}

		// Create block break event for clicked block and check if it gets cancelled
		final var break_event = new BlockBreakEvent(event.getClickedBlock(), player);
		get_module().getServer().getPluginManager().callEvent(break_event);
		if (break_event.isCancelled()) {
			return;
		}

		// Till block
		event.getClickedBlock().setType(Material.AIR);
		if (till_block(player, below)) {
			damage_item(player, item, 1);
		}
	}
}
