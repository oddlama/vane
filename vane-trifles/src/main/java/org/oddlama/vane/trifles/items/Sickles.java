package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.relative;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

public class Sickles extends Listener<Trifles> {

    @VaneItem(name = "wooden_sickle", base = Material.WOODEN_HOE, model_data = 0x760004, version = 1)
    public static class WoodenSickle extends Sickle {

        public WoodenSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 1.0;
        }

        public double config_attack_speed_def() {
            return 1.0;
        }

        public int config_harvest_radius_def() {
            return 1;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" mm", "  m", " s ")
                    .set_ingredient('m', Tag.PLANKS)
                    .set_ingredient('s', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "stone_sickle", base = Material.STONE_HOE, model_data = 0x760005, version = 1)
    public static class StoneSickle extends Sickle {

        public StoneSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 1.5;
        }

        public double config_attack_speed_def() {
            return 1.5;
        }

        public int config_harvest_radius_def() {
            return 1;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" mm", "  m", " s ")
                    .set_ingredient('m', Tag.ITEMS_STONE_TOOL_MATERIALS)
                    .set_ingredient('s', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "iron_sickle", base = Material.IRON_HOE, model_data = 0x760006, version = 1)
    public static class IronSickle extends Sickle {

        public IronSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 2.0;
        }

        public double config_attack_speed_def() {
            return 2.0;
        }

        public int config_harvest_radius_def() {
            return 2;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" mm", "  m", " s ")
                    .set_ingredient('m', Material.IRON_INGOT)
                    .set_ingredient('s', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "golden_sickle", base = Material.GOLDEN_HOE, model_data = 0x760007, version = 1)
    public static class GoldenSickle extends Sickle {

        public GoldenSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 1.5;
        }

        public double config_attack_speed_def() {
            return 3.5;
        }

        public int config_harvest_radius_def() {
            return 3;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" mm", "  m", " s ")
                    .set_ingredient('m', Material.GOLD_INGOT)
                    .set_ingredient('s', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "diamond_sickle", base = Material.DIAMOND_HOE, model_data = 0x760008, version = 1)
    public static class DiamondSickle extends Sickle {

        public DiamondSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 2.5;
        }

        public double config_attack_speed_def() {
            return 2.5;
        }

        public int config_harvest_radius_def() {
            return 2;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new ShapedRecipeDefinition("generic")
                    .shape(" mm", "  m", " s ")
                    .set_ingredient('m', Material.DIAMOND)
                    .set_ingredient('s', Material.STICK)
                    .result(key().toString())
            );
        }
    }

    @VaneItem(name = "netherite_sickle", base = Material.NETHERITE_HOE, model_data = 0x760009, version = 1)
    public static class NetheriteSickle extends Sickle {

        public NetheriteSickle(Context<Trifles> context) {
            super(context);
        }

        public double config_attack_damage_def() {
            return 3.0;
        }

        public double config_attack_speed_def() {
            return 3.0;
        }

        public int config_harvest_radius_def() {
            return 2;
        }

        @Override
        public RecipeList default_recipes() {
            return RecipeList.of(
                new SmithingRecipeDefinition("generic")
                    .base("vane_trifles:diamond_sickle")
                    .addition(Material.NETHERITE_INGOT)
                    .copy_nbt(true)
                    .result(key().toString())
            );
        }
    }

    public Sickles(Context<Trifles> context) {
        super(context.group("sickles", "Several sickles that allow players to harvest crops in a radius."));
        new WoodenSickle(get_context());
        new StoneSickle(get_context());
        new IronSickle(get_context());
        new GoldenSickle(get_context());
        new DiamondSickle(get_context());
        new NetheriteSickle(get_context());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_right_click_plant(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Only seed when right-clicking a plant
        final var root_block = event.getClickedBlock();
        final var plant_type = root_block.getType();
        if (!is_seeded_plant(plant_type)) {
            return;
        }

        // Get item variant
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof Sickle sickle) || !sickle.enabled()) {
            return;
        }

        var total_harvested = 0;
        // Harvest surroundings
        for (var relative_pos : BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(sickle.config_harvest_radius)) {
            final var block = relative(root_block, relative_pos);
            if (harvest_plant(player, block)) {
                ++total_harvested;
            }
        }

        // Damage item if we harvested at least one plant
        if (total_harvested > 0) {
            damage_item(player, item, 1 + (int) (0.25 * total_harvested));
            swing_arm(player, event.getHand());
            root_block
                .getWorld()
                .playSound(root_block.getLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 2.0f);
        }

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
