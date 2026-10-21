package org.oddlama.vane.enchantments.items

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareGrindstoneEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.CustomItemHelper
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.StorageUtil

/**
 * The Tomes class is a module group that manages the different types of tomes used for crafting
 * custom enchantments. Disabling these tomes requires corresponding adjustments to the enchantment
 * recipes.
 */
class Tomes(context: Context<Enchantments?>) :
    ModuleGroup<Enchantments?>(
        context,
        "Tomes",
        "These tomes are needed to craft custom enchantments. If you disable them here, you will need to adjust the recipes for the enchantments accordingly.",
    ) {
    init {
        GrindstoneListener(context)
        AncientTome(context)
        EnchantedAncientTome(context)
        AncientTomeOfKnowledge(context)
        EnchantedAncientTomeOfKnowledge(context)
        AncientTomeOfTheGods(context)
        EnchantedAncientTomeOfTheGods(context)
    }

    /** Represents the basic ancient tome item used for crafting and enchanting. */
    @VaneItem(name = "ancient_tome", base = Material.BOOK, modelData = 0x770000, version = 1)
    class AncientTome(context: Context<Enchantments?>) : CustomItem<Enchantments?>(context) {
        /**
         * Provides the default loot tables for the ancient tome, defining where it can be found or
         * how it can be obtained in the game.
         */
        override fun defaultLootTables(): LootTableList {
            return LootTableList.of(
                LootDefinition("generic")
                    .`in`(LootTables.ABANDONED_MINESHAFT)
                    .`in`(LootTables.BASTION_BRIDGE)
                    .`in`(LootTables.BASTION_HOGLIN_STABLE)
                    .`in`(LootTables.BASTION_OTHER)
                    .`in`(LootTables.BASTION_TREASURE)
                    .`in`(LootTables.BURIED_TREASURE)
                    .`in`(LootTables.DESERT_PYRAMID)
                    .`in`(LootTables.END_CITY_TREASURE)
                    .`in`(LootTables.FISHING_TREASURE)
                    .`in`(LootTables.IGLOO_CHEST)
                    .`in`(LootTables.JUNGLE_TEMPLE)
                    .`in`(LootTables.NETHER_BRIDGE)
                    .`in`(LootTables.PILLAGER_OUTPOST)
                    .`in`(LootTables.RUINED_PORTAL)
                    .`in`(LootTables.SHIPWRECK_TREASURE)
                    .`in`(LootTables.STRONGHOLD_LIBRARY)
                    .`in`(LootTables.UNDERWATER_RUIN_BIG)
                    .`in`(LootTables.UNDERWATER_RUIN_SMALL)
                    .`in`(LootTables.VILLAGE_TEMPLE)
                    .`in`(LootTables.WOODLAND_MANSION)
                    .add(1.0 / 5, 0, 2, key().toString()),
                LootDefinition("ancientcity")
                    .`in`(LootTables.ANCIENT_CITY)
                    .add(1.0 / 20, 0, 2, key().toString()),
                LootDefinition("terralith_generic") // terralith low
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/common"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/generic_low"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/generic_low"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/smith/novice"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/smith/novice"))
                    .`in`(
                        StorageUtil.namespacedKey(
                            "terralith",
                            "village/fortified/tavern_downstairs",
                        )
                    )
                    .`in`(
                        StorageUtil.namespacedKey(
                            "terralith",
                            "village/fortified/tavern_upstairs",
                        )
                    ) // terralith normal
                    .`in`(StorageUtil.namespacedKey("terralith", "ruin/glacial/main_cs"))
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/archer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/attic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/butcher"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/cartographer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/generic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/library"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/mason"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/smith"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/archer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/attic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/butcher"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/cartographer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/fisherman"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/food"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/generic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/library"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/mason"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/smith"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/diamond"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/emerald"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/golem"))
                    .add(1.0 / 5, 0, 2, key().toString()),
                LootDefinition("terralith_rare") // terralith rare
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/smith/expert"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/smith/expert"))
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 20, 0, 2, key().toString()),
            )
        }
    }

    /** Represents the enchanted variant of the ancient tome item. */
    @VaneItem(
        name = "enchanted_ancient_tome",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770001,
        version = 1,
    )
    class EnchantedAncientTome(context: Context<Enchantments?>) : CustomItem<Enchantments?>(context)

    /**
     * Represents the ancient tome of knowledge item, used for crafting and enchanting with a focus
     * on knowledge-related enchantments.
     */
    @VaneItem(
        name = "ancient_tome_of_knowledge",
        base = Material.BOOK,
        modelData = 0x770002,
        version = 1,
    )
    class AncientTomeOfKnowledge(context: Context<Enchantments?>) :
        CustomItem<Enchantments?>(context) {
        /**
         * Provides the default recipes for crafting the ancient tome of knowledge, using various
         * magical and knowledge-related ingredients.
         */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapelessRecipeDefinition("generic")
                    .addIngredient("vane_enchantments:ancient_tome")
                    .addIngredient(Material.FEATHER)
                    .addIngredient(Material.BLAZE_ROD)
                    .addIngredient(Material.GHAST_TEAR)
                    .result(key().toString())
            )
        }

        /**
         * Provides the default loot tables for the ancient tome of knowledge, defining where it can
         * be found or how it can be obtained in the game.
         */
        override fun defaultLootTables(): LootTableList {
            return LootTableList.of(
                LootDefinition("generic")
                    .`in`(LootTables.ABANDONED_MINESHAFT)
                    .`in`(LootTables.BASTION_TREASURE)
                    .`in`(LootTables.BURIED_TREASURE)
                    .`in`(LootTables.DESERT_PYRAMID)
                    .`in`(LootTables.NETHER_BRIDGE)
                    .`in`(LootTables.RUINED_PORTAL)
                    .`in`(LootTables.SHIPWRECK_TREASURE)
                    .`in`(LootTables.STRONGHOLD_LIBRARY)
                    .`in`(LootTables.UNDERWATER_RUIN_BIG)
                    .`in`(LootTables.VILLAGE_TEMPLE)
                    .`in`(LootTables.WOODLAND_MANSION)
                    .add(1.0 / 40, 1, 1, key().toString()),
                LootDefinition("ancientcity")
                    .`in`(LootTables.ANCIENT_CITY)
                    .add(1.0 / 30, 1, 1, key().toString()) // duplicate for more consistent spawn
                    .add(1.0 / 30, 1, 1, key().toString()),
                LootDefinition("terralith_generic") // terralith normal
                    .`in`(StorageUtil.namespacedKey("terralith", "ruin/glacial/main_cs"))
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/archer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/attic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/butcher"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/cartographer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/generic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/library"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/mason"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/smith"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/archer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/attic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/butcher"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/cartographer"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/fisherman"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/food"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/generic"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/library"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/mason"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/smith"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/diamond"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/emerald"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/treasure/golem"))
                    .add(1.0 / 40, 1, 1, key().toString()),
                LootDefinition("terralith_rare") // terralith rare
                    .`in`(StorageUtil.namespacedKey("terralith", "village/desert/smith/expert"))
                    .`in`(StorageUtil.namespacedKey("terralith", "village/fortified/smith/expert"))
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 30, 1, 1, key().toString()) // duplicate for more consistent spawn
                    .add(1.0 / 30, 1, 1, key().toString()),
            )
        }
    }

    /** Represents the enchanted variant of the ancient tome of knowledge item. */
    @VaneItem(
        name = "enchanted_ancient_tome_of_knowledge",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770003,
        version = 1,
    )
    class EnchantedAncientTomeOfKnowledge(context: Context<Enchantments?>) :
        CustomItem<Enchantments?>(context)

    /**
     * Represents the ancient tome of the gods item, a powerful tome used for crafting and
     * enchanting with divine-related enchantments.
     */
    @VaneItem(
        name = "ancient_tome_of_the_gods",
        base = Material.BOOK,
        modelData = 0x770004,
        version = 1,
    )
    class AncientTomeOfTheGods(context: Context<Enchantments?>) :
        CustomItem<Enchantments?>(context) {
        /**
         * Provides the default recipes for crafting the ancient tome of the gods, using various
         * divine and magical ingredients.
         */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" S ", "EBE", " N ")
                    .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                    .setIngredient('E', Material.ENCHANTED_BOOK)
                    .setIngredient('S', Material.NETHER_STAR)
                    .setIngredient('N', Material.NAUTILUS_SHELL)
                    .result(key().toString())
            )
        }

        /**
         * Provides the default loot tables for the ancient tome of the gods, defining where it can
         * be found or how it can be obtained in the game.
         */
        override fun defaultLootTables(): LootTableList {
            return LootTableList.of(
                LootDefinition("generic")
                    .`in`(LootTables.BASTION_TREASURE)
                    .`in`(LootTables.BURIED_TREASURE)
                    .`in`(LootTables.SHIPWRECK_TREASURE)
                    .`in`(LootTables.UNDERWATER_RUIN_BIG)
                    .add(1.0 / 200, 1, 1, key().toString()),
                LootDefinition("ancientcity")
                    .`in`(LootTables.ANCIENT_CITY)
                    .add(1.0 / 150, 1, 1, key().toString()),
                LootDefinition("terralith_generic") // terralith normal
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/treasure"))
                    .`in`(StorageUtil.namespacedKey("terralith", "underground/chest"))
                    .add(1.0 / 200, 1, 1, key().toString()),
                LootDefinition("terralith_rare") // terralith rare
                    .`in`(StorageUtil.namespacedKey("terralith", "spire/rare"))
                    .add(1.0 / 150, 1, 1, key().toString()),
            )
        }
    }

    /** Represents the enchanted variant of the ancient tome of the gods item. */
    @VaneItem(
        name = "enchanted_ancient_tome_of_the_gods",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770005,
        version = 1,
    )
    class EnchantedAncientTomeOfTheGods(context: Context<Enchantments?>) :
        CustomItem<Enchantments?>(context)

    /**
     * The GrindstoneListener class handles the grinding and disenchanting behavior for the ancient
     * tomes, ensuring that the correct item variants are used and produced during the process.
     */
    @VaneItem(
        name = "enchanted_ancient_tome_of_the_gods",
        base = Material.ENCHANTED_BOOK,
        modelData = 0x770005,
        version = 1,
    )
    class GrindstoneListener(context: Context<Enchantments?>?) : Listener<Enchantments?>(context) {
        /**
         * Listens to the PrepareGrindstoneEvent and modifies the result item if it is an enchanted
         * tome, replacing it with the corresponding normal tome.
         */
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        fun onPrepareGrindstone(event: PrepareGrindstoneEvent) {
            // Make sure to remove the enchanted variant when disenchanting a tome
            val res = event.result ?: return

            // Only if there are no enchantments on an enchanted variant, we revert to
            // non-enchanted variant
            if (res.enchantments.isNotEmpty()) {
                return
            }

            val customItem = module?.core?.itemRegistry()?.get(res)
            when (customItem) {
                is EnchantedAncientTome -> {
                    event.result = CustomItemHelper.newStack("vane_enchantments:ancient_tome")
                }

                is EnchantedAncientTomeOfKnowledge -> {
                    event.result =
                        CustomItemHelper.newStack("vane_enchantments:ancient_tome_of_knowledge")
                }

                is EnchantedAncientTomeOfTheGods -> {
                    event.result =
                        CustomItemHelper.newStack("vane_enchantments:ancient_tome_of_the_gods")
                }
            }
        }
    }
}
