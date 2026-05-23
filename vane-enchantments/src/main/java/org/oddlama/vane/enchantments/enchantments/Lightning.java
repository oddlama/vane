package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(
    name = "lightning",
    max_level = 1,
    rarity = Rarity.RARE,
    treasure = true
)
public class Lightning extends CustomEnchantment<Enchantments> {

    public Lightning(Context<Enchantments> context) {
        super(context, false);
    }

    @ConfigBoolean(
        def = true,
        desc = "Toggle lightning enchantment to cancel lightning damage for wielders of the enchant"
    )
    private boolean config_lightning_protection;

    @ConfigInt(def = 4, min = 0, max = 20, desc = "Damage modifier for the lightning enchant")
    private int config_lightning_damage;

    @ConfigBoolean(def = true, desc = "Enable lightning to work in rainstorms as well")
    private boolean config_lightning_rain;

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(
            new ShapedRecipeDefinition("generic")
                .shape("r r", "utu", " b ")
                .set_ingredient('r', Material.LIGHTNING_ROD)
                .set_ingredient('t', "vane_enchantments:ancient_tome_of_knowledge")
                .set_ingredient('b', Material.BEACON)
                .set_ingredient('u', Material.TOTEM_OF_UNDYING)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_lightning_attack(final EntityDamageEvent event) {
        // Check if an entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        // Check to see if they were struck by lightning
        if (!(event.getCause() == DamageCause.LIGHTNING)) return;

        // Check to see if lightning protection is off
        if (!config_lightning_protection) return;

        Player player = (Player) event.getEntity();
        final var item = player.getEquipment().getItemInMainHand();
        final var level = item.getEnchantmentLevel(this.bukkit());

        // If they are not holding a lightning sword, they still take the damage
        if (level == 0) return;

        // Cancel the damage to the event
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_sword_attack(final EntityDamageByEntityEvent event) {
        // Only strike when an entity is a player
        if (!(event.getDamager() instanceof Player)) return;

        // if not an attack with a weapon exit
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        Player damager = (Player) event.getDamager();
        final var damagee = event.getEntity();
        final var world = damager.getWorld();
        final var item = damager.getEquipment().getItemInMainHand();
        final var level = item.getEnchantmentLevel(this.bukkit());

        // Get enchantment level
        if (level == 0) return;

        // Get Storm status
        if (!world.hasStorm()) return;

        // Exit if config set to thunder only
        if (!config_lightning_rain && !world.isThundering()) return;

        // Test if sky is visible
        if (damagee.getLocation().getBlockY() < world.getHighestBlockYAt(damagee.getLocation())) return;

        // Execute
        event.setDamage(event.getDamage() + config_lightning_damage);
        world.strikeLightning(damagee.getLocation());
    }
}
