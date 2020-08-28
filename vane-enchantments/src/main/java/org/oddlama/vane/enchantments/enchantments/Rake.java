package org.oddlama.vane.enchantments.enchantments;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
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
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.is_replaceable_grass;
import static org.oddlama.vane.util.MaterialUtil.is_tillable;
import org.oddlama.vane.util.BlockUtil;
import static org.oddlama.vane.util.PlayerUtil.till_block;
import static org.oddlama.vane.util.BlockUtil.next_tillable_block;

@VaneEnchantment(name = "rake", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Rake extends CustomEnchantment<Enchantments> {
	public Rake(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_till_farmland(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only till additional blocks when right clicking farmland
		if (event.getClickedBlock().getType() != Material.FARMLAND) {
			return;
		}

		// Get enchantment level
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItemInMainHand();
		final var level = item.getEnchantmentLevel(this.bukkit());
		if (level == 0) {
			return;
		}

		// Get tillable block
		final var careless = item.getEnchantmentLevel(CustomEnchantment.bukkit(Careless.class)) > 0;
		final var tillable = next_tillable_block(event.getClickedBlock(), level, careless);
		if (tillable == null) {
			return;
		}

		// Till block
		if (till_block(player, tillable)) {
			damage_item(player, item, 1);
		}
	}
}
