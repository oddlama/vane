package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.Material;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.bukkit.loot.LootTables;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.AncientTomeOfTheGods;
import org.oddlama.vane.enchantments.items.BookVariant;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "hell_bent", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.ARMOR_HEAD)
public class HellBent extends CustomEnchantment<Enchantments> {
	public HellBent(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void register_recipes() {
		final var ancient_tome_of_knowledge = CustomItem.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(AncientTomeOfKnowledge.class, BookVariant.ENCHANTED_BOOK).item();

		final var recipe_key = recipe_key();
		final var item = ancient_tome_of_knowledge.clone();
		final var meta = (EnchantmentStorageMeta)item.getItemMeta();
		meta.addStoredEnchant(bukkit(), 1, false);
		item.setItemMeta(meta);
		get_module().update_enchanted_item(item);

		final var recipe = new ShapedRecipe(recipe_key, item)
			.shape(" m ",
				   " b ",
				   " t ")
			.setIngredient('b', ancient_tome_of_knowledge)
			.setIngredient('t', Material.TURTLE_HELMET)
			.setIngredient('m', Material.MUSIC_DISC_PIGSTEP);

		add_recipe(recipe);

		// Loot generation
		final var entry = new LootTableEntry(10, item);
		for (final var table : new LootTables[] {
			LootTables.BASTION_BRIDGE,
			LootTables.BASTION_HOGLIN_STABLE,
			LootTables.BASTION_OTHER,
			LootTables.BASTION_TREASURE,
		}) {
			get_module().loot_table(table).put(recipe_key, entry);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_damage(final EntityDamageEvent event) {
		final var entity = event.getEntity();
		if (!(entity instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
			return;
		}

		// Get helmet
		final var player = (Player)entity;
		final var helmet = player.getEquipment().getHelmet();
		if (helmet == null) {
			return;
		}

		// Check enchantment
		if (helmet.getEnchantmentLevel(this.bukkit()) == 0) {
			return;
		}

		event.setCancelled(true);
	}
}
