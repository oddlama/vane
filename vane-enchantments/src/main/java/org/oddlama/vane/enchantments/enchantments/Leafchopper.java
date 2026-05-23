package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "leafchopper", rarity = Rarity.COMMON, treasure = true)
public class Leafchopper extends CustomEnchantment<Enchantments> {

    public Leafchopper(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape(" s ", "sbs", " s ")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
                .set_ingredient('s', Material.SHEARS)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_left_click_leaves(PlayerInteractEvent event) {
        if (
            !event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.LEFT_CLICK_BLOCK
        ) {
            return;
        }

        // Check leaves
        var block = event.getClickedBlock();
        var data = block.getBlockData();
        if (!(data instanceof Leaves)) {
            return;
        }

        // Check non persistent leaves
        var leaves = (Leaves) data;
        if (leaves.isPersistent()) {
            return;
        }

        // Check enchantment level
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItemInMainHand();
        final var level = item.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            return;
        }

        // Break instantly, for no additional durability cost.
        block.breakNaturally();
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }
}
