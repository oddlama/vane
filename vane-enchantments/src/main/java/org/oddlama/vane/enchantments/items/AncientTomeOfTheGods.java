package org.oddlama.vane.enchantments.items;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneItem(name = "ancient_tome_of_the_gods")
public class AncientTomeOfTheGods extends CustomItem<Enchantments, AncientTomeOfTheGods> {

	public static class AncientTomeOfTheGodsVariant
		extends CustomItemVariant<Enchantments, AncientTomeOfTheGods, BookVariant> {

		public AncientTomeOfTheGodsVariant(AncientTomeOfTheGods parent, BookVariant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
				if (variant() == BookVariant.BOOK) {
				final var recipe_key = recipe_key();
				final var item = item();

				final var ancient_tome_of_knowledge = CustomItem
					.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(
						AncientTomeOfKnowledge.class,
						variant()
					)
					.item();
				final var recipe = new ShapedRecipe(recipe_key, item)
					.shape(" s ", "ebe", " n ")
					.setIngredient('b', ancient_tome_of_knowledge)
					.setIngredient('e', Material.ENCHANTED_BOOK)
					.setIngredient('s', Material.NETHER_STAR)
					.setIngredient('n', Material.NAUTILUS_SHELL);

				add_recipe(recipe);

				final var entry = new LootTableEntry(200, item);
				for (final var table : new LootTables[] {
					LootTables.BASTION_TREASURE,
					LootTables.BURIED_TREASURE,
					LootTables.SHIPWRECK_TREASURE,
					LootTables.UNDERWATER_RUIN_BIG,
				}) {
					get_module().loot_table(table).put(recipe_key, entry);
				}

				get_module().loot_table(LootTables.CLERIC_GIFT).put(recipe_key, new LootTableEntry(50, item));
			}
		}

		@Override
		public Material base() {
			switch (variant()) {
				default:
					throw new RuntimeException("Missing variant case. This is a bug.");
				case BOOK:
					return Material.BOOK;
				case ENCHANTED_BOOK:
					return Material.ENCHANTED_BOOK;
			}
		}
	}

	public AncientTomeOfTheGods(Context<Enchantments> context) {
		super(context, BookVariant.class, BookVariant.values(), AncientTomeOfTheGodsVariant::new);
	}
}
