package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.Conversions.exp_for_level;
import static org.oddlama.vane.util.PlayerUtil.give_item;
import static org.oddlama.vane.util.PlayerUtil.remove_one_item_from_hand;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.items.XpBottles.XpBottle;

@VaneItem(name = "empty_xp_bottle", base = Material.GLASS_BOTTLE, model_data = 0x76000a, version = 1)
public class EmptyXpBottle extends CustomItem<Trifles> {

    @ConfigDouble(
        def = 0.3,
        min = 0.0,
        max = 0.999,
        desc = "Percentage of experience lost when bottling. For 10% loss, bottling 30 levels will require 30 * (1 / (1 - 0.1)) = 33.33 levels"
    )
    public double config_loss_percentage;

    public EmptyXpBottle(Context<Trifles> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapelessRecipeDefinition("generic")
                .add_ingredient(Material.EXPERIENCE_BOTTLE)
                .add_ingredient(Material.GLASS_BOTTLE)
                .add_ingredient(Material.GOLD_NUGGET)
                .result(key().toString())
        );
    }

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.TEMPT, InhibitBehavior.USE_OFFHAND);
    }

    public static int get_total_exp(final Player player) {
        return level_to_exp(player.getLevel()) + Math.round(player.getExpToLevel() * player.getExp());
    }

    public static int level_to_exp(int level) {
        // Formulas taken from: https://minecraft.fandom.com/wiki/Experience#Leveling_up
        if (level > 30) {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        if (level > 15) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return level * level + 6 * level;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
    public void on_player_right_click(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Get item variant
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        if (!isInstance(item)) {
            return;
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY);

        switch (event.getAction()) {
            default:
                return;
            case RIGHT_CLICK_AIR:
                break;
            case RIGHT_CLICK_BLOCK:
                // Require non-canceled state (so it won't trigger for block-actions like chests)
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return;
                }
                break;
        }

        // Check if last consume time is too recent, to prevent accidental re-filling
        final var now = System.currentTimeMillis();
        final var last_consume = get_module().last_xp_bottle_consume_time.getOrDefault(player.getUniqueId(), 0l);
        if (now - last_consume < 1000) {
            return;
        }

        // Find maximum fitting capacity
        XpBottle xp_bottle = null;
        int exp = 0;
        for (final var bottle : get_module().xp_bottles.bottles) {
            var cur_exp = (int) ((1.0 / (1.0 - config_loss_percentage)) * exp_for_level(bottle.config_capacity));

            // Check if player has enough xp and this variant has more than the last
            if (get_total_exp(player) >= cur_exp && cur_exp > exp) {
                exp = cur_exp;
                xp_bottle = bottle;
            }
        }

        // Check if there was a fitting bottle
        if (xp_bottle == null) {
            return;
        }

        // Take xp, take item, play sound, give item.
        player.giveExp(-exp, false);
        remove_one_item_from_hand(player, event.getHand());
        give_item(player, xp_bottle.newStack());
        player
            .getWorld()
            .playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 4.0f);
        swing_arm(player, event.getHand());
    }
}
