package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.next_seedable_block;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.farmland_for;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.MaterialUtil.seed_for;
import static org.oddlama.vane.util.PlayerUtil.seed_block;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import com.destroystokyo.paper.MaterialTags;

import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.BookVariant;

@VaneEnchantment(name = "seeding", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Seeding extends CustomEnchantment<Enchantments> {
	public Seeding(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void register_recipes() {
		final var ancient_tome_of_knowledge_enchanted = CustomItem.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(AncientTomeOfKnowledge.class, BookVariant.ENCHANTED_BOOK).item();
		final var ancient_tome_of_knowledge = CustomItem.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(AncientTomeOfKnowledge.class, BookVariant.BOOK).item();

		final var recipe_key = recipe_key();
		final var item = ancient_tome_of_knowledge_enchanted.clone();
		final var meta = (EnchantmentStorageMeta)item.getItemMeta();
		meta.addStoredEnchant(bukkit(), 1, false);
		item.setItemMeta(meta);
		get_module().update_enchanted_item(item);

		final var recipe = new ShapedRecipe(recipe_key, item)
			.shape("1 7",
				   "2b6",
				   "345")
			.setIngredient('b', ancient_tome_of_knowledge)
			.setIngredient('1', Material.PUMPKIN_SEEDS)
			.setIngredient('2', Material.CARROT)
			.setIngredient('3', Material.WHEAT_SEEDS)
			.setIngredient('4', Material.NETHER_WART)
			.setIngredient('5', Material.BEETROOT_SEEDS)
			.setIngredient('6', Material.POTATO)
			.setIngredient('7', Material.MELON_SEEDS);

		add_recipe(recipe);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click_plant(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only seed when right clicking a plant
		final var plant_type = event.getClickedBlock().getType();
		if (!is_seeded_plant(plant_type)) {
			return;
		}

		// Get enchantment level
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
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
		if (seed_block(player, item, seedable, plant_type, seed_type)) {
			damage_item(player, item, 1);
			swing_arm(player, event.getHand());
		}
	}
}
