package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.Tag
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.MaterialUtil
import org.oddlama.vane.util.PlayerUtil

/**
 * Registers all sickle item tiers and handles area crop harvesting behavior.
 *
 * @param context module context used for registration.
 */
class Sickles(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group("Sickles", "Several sickles that allow players to harvest crops in a radius.")
    ) {
    @VaneItem(name = "wooden_sickle", base = Material.WOODEN_HOE, modelData = 0x760004, version = 1)
    /** Wooden sickle tier. */
    class WoodenSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the wooden sickle. */
        fun configAttackDamageDef(): Double {
            return 1.0
        }

        /** Default attack speed for the wooden sickle. */
        fun configAttackSpeedDef(): Double {
            return 1.0
        }

        /** Default harvest radius for the wooden sickle. */
        fun configHarvestRadiusDef(): Int {
            return 1
        }

        /** Defines the wooden sickle crafting recipe. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.PLANKS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "stone_sickle", base = Material.STONE_HOE, modelData = 0x760005, version = 1)
    /** Stone sickle tier. */
    class StoneSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the stone sickle. */
        fun configAttackDamageDef(): Double {
            return 1.5
        }

        /** Default attack speed for the stone sickle. */
        fun configAttackSpeedDef(): Double {
            return 1.5
        }

        /** Default harvest radius for the stone sickle. */
        fun configHarvestRadiusDef(): Int {
            return 1
        }

        /** Defines the stone sickle crafting recipe. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Tag.ITEMS_STONE_TOOL_MATERIALS)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "iron_sickle", base = Material.IRON_HOE, modelData = 0x760006, version = 1)
    /** Iron sickle tier. */
    class IronSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the iron sickle. */
        fun configAttackDamageDef(): Double {
            return 2.0
        }

        /** Default attack speed for the iron sickle. */
        fun configAttackSpeedDef(): Double {
            return 2.0
        }

        /** Default harvest radius for the iron sickle. */
        fun configHarvestRadiusDef(): Int {
            return 2
        }

        /** Defines the iron sickle crafting recipe. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.IRON_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(name = "golden_sickle", base = Material.GOLDEN_HOE, modelData = 0x760007, version = 1)
    /** Golden sickle tier. */
    class GoldenSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the golden sickle. */
        fun configAttackDamageDef(): Double {
            return 1.5
        }

        /** Default attack speed for the golden sickle. */
        fun configAttackSpeedDef(): Double {
            return 3.5
        }

        /** Default harvest radius for the golden sickle. */
        fun configHarvestRadiusDef(): Int {
            return 3
        }

        /** Defines the golden sickle crafting recipe. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.GOLD_INGOT)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(
        name = "diamond_sickle",
        base = Material.DIAMOND_HOE,
        modelData = 0x760008,
        version = 1,
    )
    /** Diamond sickle tier. */
    class DiamondSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the diamond sickle. */
        fun configAttackDamageDef(): Double {
            return 2.5
        }

        /** Default attack speed for the diamond sickle. */
        fun configAttackSpeedDef(): Double {
            return 2.5
        }

        /** Default harvest radius for the diamond sickle. */
        fun configHarvestRadiusDef(): Int {
            return 2
        }

        /** Defines the diamond sickle crafting recipe. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                ShapedRecipeDefinition("generic")
                    .shape(" MM", "  M", " S ")
                    .setIngredient('M', Material.DIAMOND)
                    .setIngredient('S', Material.STICK)
                    .result(key().toString())
            )
        }
    }

    @VaneItem(
        name = "netherite_sickle",
        base = Material.NETHERITE_HOE,
        modelData = 0x760009,
        version = 1,
    )
    /** Netherite sickle tier. */
    class NetheriteSickle(context: Context<Trifles?>) : Sickle(context) {
        /** Default attack damage for the netherite sickle. */
        fun configAttackDamageDef(): Double {
            return 3.0
        }

        /** Default attack speed for the netherite sickle. */
        fun configAttackSpeedDef(): Double {
            return 3.0
        }

        /** Default harvest radius for the netherite sickle. */
        fun configHarvestRadiusDef(): Int {
            return 2
        }

        /** Defines the netherite upgrade recipe from a diamond sickle. */
        override fun defaultRecipes(): RecipeList {
            return RecipeList.of(
                SmithingRecipeDefinition("generic")
                    .base("vane_trifles:diamond_sickle")
                    .addition(Material.NETHERITE_INGOT)
                    .copyNbt(true)
                    .result(key().toString())
            )
        }
    }

    /** Registers all sickle item tiers. */
    init {
        val context = requireNotNull(getContext())
        WoodenSickle(context)
        StoneSickle(context)
        IronSickle(context)
        GoldenSickle(context)
        DiamondSickle(context)
        NetheriteSickle(context)
    }

    /** Harvests all seeded plants around the clicked crop based on sickle radius. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerRightClickPlant(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Continue only for seeded plants.
        val rootBlock = event.clickedBlock ?: return
        val plantType = rootBlock.type
        if (!MaterialUtil.isSeededPlant(plantType)) {
            return
        }

        // Require a matching enabled sickle item.
        val player = event.player
        val hand = event.hand ?: return
        val item = player.equipment.getItem(hand)
        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
        if (customItem !is Sickle || !customItem.enabled()) {
            return
        }

        var totalHarvested = 0
        // Harvest nearby blocks within configured radius.
        for (relativePos in
            BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[customItem.configHarvestRadius]) {
            val block = BlockUtil.relative(rootBlock, relativePos)
            if (PlayerUtil.harvestPlant(player, block)) {
                ++totalHarvested
            }
        }

        // Consume durability only when at least one crop was harvested.
        if (totalHarvested > 0) {
            ItemUtil.damageItem(player, item, 1 + (0.25 * totalHarvested).toInt())
            PlayerUtil.swingArm(player, hand)
            rootBlock.world.playSound(
                rootBlock.location,
                Sound.ITEM_HOE_TILL,
                SoundCategory.BLOCKS,
                1.0f,
                2.0f,
            )
        }

        // Prevent offhand actions from triggering on the same click.
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }
}
