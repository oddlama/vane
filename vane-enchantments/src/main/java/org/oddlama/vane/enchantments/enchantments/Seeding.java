package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.next_seedable_block;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.farmland_for;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.MaterialUtil.seed_for;
import static org.oddlama.vane.util.PlayerUtil.seed_block;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(
    name = "seeding",
    max_level = 4,
    rarity = Rarity.COMMON,
    treasure = true
)
public class Seeding extends CustomEnchantment<Enchantments> {

    public Seeding(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("1 7", "2b6", "345")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
                .set_ingredient('1', Material.PUMPKIN_SEEDS)
                .set_ingredient('2', Material.CARROT)
                .set_ingredient('3', Material.WHEAT_SEEDS)
                .set_ingredient('4', Material.NETHER_WART)
                .set_ingredient('5', Material.BEETROOT_SEEDS)
                .set_ingredient('6', Material.POTATO)
                .set_ingredient('7', Material.MELON_SEEDS)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_right_click_plant(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only seed when right-clicking a plant
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
