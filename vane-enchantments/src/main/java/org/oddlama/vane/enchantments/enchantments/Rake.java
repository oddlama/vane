package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.BlockUtil.next_tillable_block;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.till_block;

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

@VaneEnchantment(name = "rake", max_level = 4, rarity = Rarity.COMMON, treasure = true)
public class Rake extends CustomEnchantment<Enchantments> {

    public Rake(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" h ", "hbh", " h ")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
                .set_ingredient('h', Material.GOLDEN_HOE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_till_farmland(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only till additional blocks when right-clicking farmland
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
        final var tillable = next_tillable_block(event.getClickedBlock(), level, true);
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
