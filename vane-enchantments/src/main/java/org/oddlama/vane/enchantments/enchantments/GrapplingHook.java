package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

import java.util.List;

@VaneEnchantment(
    name = "grappling_hook",
    max_level = 3,
    rarity = Rarity.UNCOMMON,
    treasure = true
)
public class GrapplingHook extends CustomEnchantment<Enchantments> {

    // Constant offset to the added velocity, so the player will always move up a little.
    private static final Vector CONSTANT_OFFSET = new Vector(0.0, 0.2, 0.0);
    private static final double BOUNDING_BOX_RADIUS = 0.2;

    @ConfigDouble(
        def = 16.0,
        min = 2.0,
        max = 50.0,
        desc = "Ideal grappling distance for maximum grapple strength. Strength increases rapidly before, and falls of slowly after."
    )
    private double config_ideal_distance;

    @ConfigDoubleList(def = { 1.6, 2.1, 2.7 }, min = 0.0, desc = "Grappling strength for each enchantment level.")
    private List<Double> config_strength;

    public GrapplingHook(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("h", "l", "b")
                .set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
                .set_ingredient('l', Material.LEAD)
                .set_ingredient('h', Material.TRIPWIRE_HOOK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    private double get_strength(int level) {
        if (level > 0 && level <= config_strength.size()) {
            return config_strength.get(level - 1);
        }
        return config_strength.get(0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_fish_event(final PlayerFishEvent event) {
        // Get enchantment level
        final var player = event.getPlayer();
        var item = player.getEquipment().getItemInMainHand();
        var level = item.getEnchantmentLevel(this.bukkit());
        if (level == 0) {
            item = player.getEquipment().getItemInOffHand();
            level = item.getEnchantmentLevel(this.bukkit());
            if (level == 0) {
                return;
            }
        }

        switch (event.getState()) {
            case IN_GROUND:
                break;
            case REEL_IN:
                // Check if the hook is colliding with blocks, worldborder, or entities
                // We won't activate the grappling hook if it collides with an entity because the event
                // would instead be in the CAUGHT_ENTITY state
                double hook_x = event.getHook().getLocation().getX();
                double hook_y = event.getHook().getLocation().getY();
                double hook_z = event.getHook().getLocation().getZ();
                if (!event.getHook().wouldCollideUsing(new BoundingBox(
                        hook_x - BOUNDING_BOX_RADIUS, hook_y - BOUNDING_BOX_RADIUS, hook_z - BOUNDING_BOX_RADIUS,
                        hook_x + BOUNDING_BOX_RADIUS, hook_y + BOUNDING_BOX_RADIUS, hook_z + BOUNDING_BOX_RADIUS))
                ) { return; }
                break;
            default:
                return;
        }

        var direction = event.getHook().getLocation().subtract(player.getLocation()).toVector();
        var distance = direction.length();
        var attenuation = distance / config_ideal_distance;

        // Reset fall distance
        player.setFallDistance(0.0f);

        var vector_multiplier = get_strength(level) * Math.exp(1.0 - attenuation) * attenuation;
        var adjusted_vector = direction.normalize().multiply(vector_multiplier).add(CONSTANT_OFFSET);

        // If the hook is below the player, set the Y component to 0.0 and only add the constant offset.
        // This prevents the player from just sliding against the ground when the hook is below them.
        if (player.getY() - event.getHook().getY() > 0) {
            adjusted_vector.setY(0.0).add(CONSTANT_OFFSET);
        }

        // Set player velocity
        player.setVelocity(player.getVelocity().add(adjusted_vector));
    }
}
