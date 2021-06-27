package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.next_tillable_block;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.till_block;

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

@VaneEnchantment(name = "rake", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Rake extends CustomEnchantment<Enchantments> {
	public Rake(Context<Enchantments> context) {
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
			.shape(" h ",
				   "hbh",
				   " h ")
			.setIngredient('b', ancient_tome_of_knowledge)
			.setIngredient('h', Material.GOLDEN_HOE);

		add_recipe(recipe);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_till_farmland(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only till additional blocks when right clicking farmland
		if (event.getClickedBlock().getType() != Material.FARMLAND) {
			return;
		}

		// Get enchantment level
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
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
			swing_arm(player, event.getHand());
		}
	}
}
