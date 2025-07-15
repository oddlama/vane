package org.oddlama.vane.enchantments.enchantments;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "angel", max_level = 5, rarity = Rarity.VERY_RARE, treasure = true, allow_custom = true)
public class Angel extends CustomEnchantment<Enchantments> {

    @ConfigDouble(
        def = 0.1,
        min = 0.0,
        max = 1.0,
        desc = "Acceleration percentage. Each tick, the current flying speed is increased X percent towards the target speed. Low values (~0.1) typically result in a smooth acceleration curve and a natural feeling."
    )
    private double config_acceleration_percentage;

    @ConfigDoubleList(
        def = { 0.7, 1.1, 1.4, 1.7, 2.0 },
        min = 0.0,
        desc = "Flying speed in blocks per second for each enchantment level."
    )
    private List<Double> config_speed;

    public Angel(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("prp", "mbm", "mdm")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_the_gods")
                .set_ingredient('m', Material.PHANTOM_MEMBRANE)
                .set_ingredient('d', Material.DRAGON_BREATH)
                .set_ingredient('p', Material.PUFFERFISH_BUCKET)
                .set_ingredient('r', Material.FIREWORK_ROCKET)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    @Override
    public LootTableList default_loot_tables() {
        return LootTableList.of(
            new LootDefinition("generic")
                .in(LootTables.BURIED_TREASURE)
                .in(LootTables.PILLAGER_OUTPOST)
                .in(LootTables.RUINED_PORTAL)
                .in(LootTables.STRONGHOLD_LIBRARY)
                .in(LootTables.UNDERWATER_RUIN_BIG)
                .in(LootTables.VILLAGE_TEMPLE)
                .add(1.0 / 250, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        );
    }

    private double get_speed(int level) {
        if (level > 0 && level <= config_speed.size()) {
            return config_speed.get(level - 1);
        }
        return config_speed.get(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_move(final PlayerMoveEvent event) {
        // Check sneaking and flying
        final var player = event.getPlayer();
        if (!player.isSneaking() || !player.isGliding()) {
            return;
        }

        // Check enchantment level
        final var chest = player.getEquipment().getChestplate();
        if (chest == null) { // Can happen due to other plugins
            return;
        }
        final var level = chest.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            return;
        }

        final var loc = player.getLocation();
        final var dir = loc.getDirection();
        if (dir.length() == 0) {
            return;
        }

        // Scale the delta dependent on the angle. Higher angle -> less effect
        final var vel = player.getVelocity();
        final var delta = config_acceleration_percentage * (1.0 - dir.angle(vel) / Math.PI);
        final var factor = get_speed(level);

        // Exponential moving average between velocity and target velocity
        final var new_vel = vel.multiply(1.0 - delta).add(dir.normalize().multiply(delta * factor));
        player.setVelocity(new_vel);

        // Spawn particles
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 0, -new_vel.getX(), -new_vel.getY(), -new_vel.getZ(), 0.4);
    }
}
