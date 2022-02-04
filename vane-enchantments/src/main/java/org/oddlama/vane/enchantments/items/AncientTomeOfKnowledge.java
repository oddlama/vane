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

import java.util.Map;

@VaneItem(name = "ancient_tome_of_knowledge")
public class AncientTomeOfKnowledge extends CustomItem<Enchantments, AncientTomeOfKnowledge> {

	public static class AncientTomeOfKnowledgeVariant
		extends CustomItemVariant<Enchantments, AncientTomeOfKnowledge, BookVariant> {

		public AncientTomeOfKnowledgeVariant(AncientTomeOfKnowledge parent, BookVariant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			if (variant() == BookVariant.BOOK) {
				final var recipe_key = recipe_key();
				final var item = item();

				final var ancient_tome = CustomItem
					.<AncientTome.AncientTomeVariant>variant_of(AncientTome.class, variant())
					.item();
				final var recipe = new ShapedRecipe(recipe_key, item)
					.shape("fb", "rg")
					.setIngredient('f', Material.FEATHER)
					.setIngredient('b', ancient_tome)
					.setIngredient('r', Material.BLAZE_ROD)
					.setIngredient('g', Material.GHAST_TEAR);

				add_recipe(recipe);

				final var entry = new LootTableEntry(40, item);
				for (final var table : new LootTables[] {
					LootTables.ABANDONED_MINESHAFT,
					LootTables.BASTION_TREASURE,
					LootTables.BURIED_TREASURE,
					LootTables.DESERT_PYRAMID,
					LootTables.NETHER_BRIDGE,
					LootTables.RUINED_PORTAL,
					LootTables.SHIPWRECK_TREASURE,
					LootTables.STRONGHOLD_LIBRARY,
					LootTables.UNDERWATER_RUIN_BIG,
					LootTables.VILLAGE_TEMPLE,
					LootTables.WOODLAND_MANSION,
				}) {
					get_module().loot_table(table).put(recipe_key, entry);
				}
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

	public AncientTomeOfKnowledge(Context<Enchantments> context) {
		super(context, BookVariant.class, BookVariant.values(), AncientTomeOfKnowledgeVariant::new);
	}
}
