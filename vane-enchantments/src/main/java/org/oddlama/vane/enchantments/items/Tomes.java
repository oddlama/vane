package org.oddlama.vane.enchantments.items;

import org.bukkit.Material;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.util.StorageUtil;

public class Tomes extends ModuleGroup<Enchantments> {
	public Tomes(Context<Enchantments> context) {
		super(context, "tomes", "These tomes are needed to craft custom enchantments. If you disable them here, you will need to adjust the recipes for the enchantments accordingly.");
		new AncientTome(get_context());
		new EnchantedAncientTome(get_context());
		new AncientTomeOfKnowledge(get_context());
		new EnchantedAncientTomeOfKnowledge(get_context());
		new AncientTomeOfTheGods(get_context());
		new EnchantedAncientTomeOfTheGods(get_context());
	}

	@VaneItem(name = "ancient_tome", base = Material.BOOK, model_data = 0x770000, version = 1)
	public static class AncientTome extends CustomItem<Enchantments> {
		public AncientTome(Context<Enchantments> context) { super(context); }

		@Override
		public LootTableList default_loot_tables() {
			return LootTableList.of(
				new LootDefinition("generic")
					.in(LootTables.ABANDONED_MINESHAFT)
					.in(LootTables.BASTION_BRIDGE)
					.in(LootTables.BASTION_HOGLIN_STABLE)
					.in(LootTables.BASTION_OTHER)
					.in(LootTables.BASTION_TREASURE)
					.in(LootTables.BURIED_TREASURE)
					.in(LootTables.DESERT_PYRAMID)
					.in(LootTables.END_CITY_TREASURE)
					.in(LootTables.FISHING_TREASURE)
					.in(LootTables.IGLOO_CHEST)
					.in(LootTables.JUNGLE_TEMPLE)
					.in(LootTables.NETHER_BRIDGE)
					.in(LootTables.PILLAGER_OUTPOST)
					.in(LootTables.RUINED_PORTAL)
					.in(LootTables.SHIPWRECK_TREASURE)
					.in(LootTables.STRONGHOLD_LIBRARY)
					.in(LootTables.UNDERWATER_RUIN_BIG)
					.in(LootTables.UNDERWATER_RUIN_SMALL)
					.in(LootTables.VILLAGE_TEMPLE)
					.in(LootTables.WOODLAND_MANSION)
					.add(1.0 / 5, 0, 2, key().toString()),
				new LootDefinition("ancientcity")
					.in(LootTables.ANCIENT_CITY)
					.add(1.0 / 20, 0, 2, key().toString()),
				new LootDefinition("terralith_generic")
					// terralith low
					.in(StorageUtil.namespaced_key("terralith", "spire/common"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/generic_low"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/generic_low"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/smith/novice"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/smith/novice"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/tavern_downstairs"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/tavern_upstairs"))
					// terralith normal
					.in(StorageUtil.namespaced_key("terralith", "ruin/glacial/main_cs"))
					.in(StorageUtil.namespaced_key("terralith", "spire/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "underground/chest"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/archer"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/attic"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/butcher"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/cartographer"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/generic"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/library"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/mason"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/smith"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/archer"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/attic"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/butcher"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/cartographer"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/fisherman"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/food"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/generic"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/library"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/mason"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/smith"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/diamond"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/emerald"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/golem"))
					.add(1.0 / 5, 0, 2, key().toString()),
				new LootDefinition("terralith_rare")
					// terralith rare
					.in(StorageUtil.namespaced_key("terralith", "village/desert/smith/expert"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/smith/expert"))
					.in(StorageUtil.namespaced_key("terralith", "spire/rare"))
					.add(1.0 / 20, 0, 2, key().toString())
				);
		}
	}

	@VaneItem(name = "enchanted_ancient_tome", base = Material.ENCHANTED_BOOK, model_data = 0x770001, version = 1)
	public static class EnchantedAncientTome extends CustomItem<Enchantments> {
		public EnchantedAncientTome(Context<Enchantments> context) { super(context); }
	}

	@VaneItem(name = "ancient_tome_of_knowledge", base = Material.BOOK, model_data = 0x770002, version = 1)
	public static class AncientTomeOfKnowledge extends CustomItem<Enchantments> {
		public AncientTomeOfKnowledge(Context<Enchantments> context) { super(context); }

		@Override
		public RecipeList default_recipes() {
			return RecipeList.of(new ShapelessRecipeDefinition("generic")
				.add_ingredient("vane_enchantments:ancient_tome")
				.add_ingredient(Material.FEATHER)
				.add_ingredient(Material.BLAZE_ROD)
				.add_ingredient(Material.GHAST_TEAR)
				.result(key().toString()));
		}

		@Override
		public LootTableList default_loot_tables() {
			return LootTableList.of(
				new LootDefinition("generic")
					.in(LootTables.ABANDONED_MINESHAFT)
					.in(LootTables.BASTION_TREASURE)
					.in(LootTables.BURIED_TREASURE)
					.in(LootTables.DESERT_PYRAMID)
					.in(LootTables.NETHER_BRIDGE)
					.in(LootTables.RUINED_PORTAL)
					.in(LootTables.SHIPWRECK_TREASURE)
					.in(LootTables.STRONGHOLD_LIBRARY)
					.in(LootTables.UNDERWATER_RUIN_BIG)
					.in(LootTables.VILLAGE_TEMPLE)
					.in(LootTables.WOODLAND_MANSION)
					.add(1.0 / 40, 1, 1, key().toString()),
				new LootDefinition("ancientcity")
					.in(LootTables.ANCIENT_CITY)
					.add(1.0 / 30, 1, 1, key().toString())  //duplicate for more consistent spawn
					.add(1.0 / 30, 1, 1, key().toString()),
				new LootDefinition("terralith_generic")
					// terralith normal
					.in(StorageUtil.namespaced_key("terralith", "ruin/glacial/main_cs"))
					.in(StorageUtil.namespaced_key("terralith", "spire/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "underground/chest"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/archer"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/attic"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/butcher"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/cartographer"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/generic"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/library"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/mason"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/smith"))
					.in(StorageUtil.namespaced_key("terralith", "village/desert/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/archer"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/attic"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/butcher"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/cartographer"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/fisherman"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/food"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/generic"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/library"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/mason"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/smith"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/diamond"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/emerald"))
					.in(StorageUtil.namespaced_key("terralith", "village/treasure/golem"))
					.add(1.0 / 40, 1, 1, key().toString()),
				new LootDefinition("terralith_rare")
					// terralith rare
					.in(StorageUtil.namespaced_key("terralith", "village/desert/smith/expert"))
					.in(StorageUtil.namespaced_key("terralith", "village/fortified/smith/expert"))
					.in(StorageUtil.namespaced_key("terralith", "spire/rare"))
					.add(1.0 / 30, 1, 1, key().toString())  //duplicate for more consistent spawn
					.add(1.0 / 30, 1, 1, key().toString())
				);
		}
	}

	@VaneItem(name = "enchanted_ancient_tome_of_knowledge", base = Material.ENCHANTED_BOOK, model_data = 0x770003, version = 1)
	public static class EnchantedAncientTomeOfKnowledge extends CustomItem<Enchantments> {
		public EnchantedAncientTomeOfKnowledge(Context<Enchantments> context) { super(context); }
	}

	@VaneItem(name = "ancient_tome_of_the_gods", base = Material.BOOK, model_data = 0x770004, version = 1)
	public static class AncientTomeOfTheGods extends CustomItem<Enchantments> {
		public AncientTomeOfTheGods(Context<Enchantments> context) { super(context); }

		@Override
		public RecipeList default_recipes() {
			return RecipeList.of(new ShapedRecipeDefinition("generic")
				.shape(" s ", "ebe", " n ")
				.set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
				.set_ingredient('e', Material.ENCHANTED_BOOK)
				.set_ingredient('s', Material.NETHER_STAR)
				.set_ingredient('n', Material.NAUTILUS_SHELL)
				.result(key().toString()));
		}

		@Override
		public LootTableList default_loot_tables() {
			return LootTableList.of(
				new LootDefinition("generic")
					.in(LootTables.BASTION_TREASURE)
					.in(LootTables.BURIED_TREASURE)
					.in(LootTables.SHIPWRECK_TREASURE)
					.in(LootTables.UNDERWATER_RUIN_BIG)
					.add(1.0 / 200, 1, 1, key().toString()),
				new LootDefinition("ancientcity")
					.in(LootTables.ANCIENT_CITY)
					.add(1.0 / 150, 1, 1, key().toString()),
				new LootDefinition("terralith_generic")
					// terralith normal
					.in(StorageUtil.namespaced_key("terralith", "spire/treasure"))
					.in(StorageUtil.namespaced_key("terralith", "underground/chest"))
					.add(1.0 / 200, 1, 1, key().toString()),
				new LootDefinition("terralith_rare")
					// terralith rare
					.in(StorageUtil.namespaced_key("terralith", "spire/rare"))
					.add(1.0 / 150, 1, 1, key().toString())
				);
		}
	}

	@VaneItem(name = "enchanted_ancient_tome_of_the_gods", base = Material.ENCHANTED_BOOK, model_data = 0x770005, version = 1)
	public static class EnchantedAncientTomeOfTheGods extends CustomItem<Enchantments> {
		public EnchantedAncientTomeOfTheGods(Context<Enchantments> context) { super(context); }
	}
}
