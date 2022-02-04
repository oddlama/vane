package org.oddlama.vane.enchantments.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneItem(name = "ancient_tome")
public class AncientTome extends CustomItem<Enchantments, AncientTome> {

	public static class AncientTomeVariant extends CustomItemVariant<Enchantments, AncientTome, BookVariant> {

		public AncientTomeVariant(AncientTome parent, BookVariant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			if (variant() == BookVariant.BOOK) {
				final var recipe_key = recipe_key();
				final var item = item();
				final var entry = new LootTableEntry(5, item, 0, 2);

				for (final var table : new LootTables[] {
					LootTables.ABANDONED_MINESHAFT,
					LootTables.BASTION_BRIDGE,
					LootTables.BASTION_HOGLIN_STABLE,
					LootTables.BASTION_OTHER,
					LootTables.BASTION_TREASURE,
					LootTables.BURIED_TREASURE,
					LootTables.DESERT_PYRAMID,
					LootTables.END_CITY_TREASURE,
					LootTables.FISHING_TREASURE,
					LootTables.IGLOO_CHEST,
					LootTables.JUNGLE_TEMPLE,
					LootTables.NETHER_BRIDGE,
					LootTables.PILLAGER_OUTPOST,
					LootTables.RUINED_PORTAL,
					LootTables.SHIPWRECK_TREASURE,
					LootTables.STRONGHOLD_LIBRARY,
					LootTables.UNDERWATER_RUIN_BIG,
					LootTables.UNDERWATER_RUIN_SMALL,
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

	public AncientTome(Context<Enchantments> context) {
		super(context, BookVariant.class, BookVariant.values(), AncientTomeVariant::new);
	}
}
