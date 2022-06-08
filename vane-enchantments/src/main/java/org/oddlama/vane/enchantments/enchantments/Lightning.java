package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.ItemUtil.damage_item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent; 
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;
import org.bukkit.WeatherType;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.config.loot.LootDefinition;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

import com.destroystokyo.paper.MaterialTags;

@VaneEnchantment(
    name = "lightning", 
    max_level = 1, 
    rarity = Rarity.RARE, 
    treasure = true, 
    target = EnchantmentTarget.WEAPON
)
public class Lightning extends CustomEnchantment<Enchantments> {

    public Lightning(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(new ShapedRecipeDefinition("generic")
        .shape("r r","utu"," b ")
        .set_ingredient('r', Material.LIGHTNING_ROD)
        .set_ingredient('t', "vane_enchantments:ancient_tome_of_knowledge")
        .set_ingredient('b', Material.BEACON)
        .set_ingredient('u', Material.TOTEM_OF_UNDYING)
        .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge")));
    }

    @Override
    public boolean can_enchant(@NotNull ItemStack item_stack) {
        return MaterialTags.SWORDS.isTagged(item_stack);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_sword_attack(final EntityDamageByEntityEvent event) {
        
        // Only strike when entity is a player
        if(!(event.getDamager() instanceof Player)){
            return;
        }
        Player damager = (Player) event.getDamager();
        final var damagee = event.getEntity();
        final var world = damager.getWorld();
        final var item = damager.getEquipment().getItemInMainHand();
        final var level = item.getEnchantmentLevel(this.bukkit());

        // Get enchantment level
        if (level == 0){
            return;
        }

        // Get Storm status
        if(!world.hasStorm()){
            return;
        }

        // Test if sky is visible
        if(damager.getLocation().getBlockY() < world.getHighestBlockYAt(damager.getLocation()))  {
            return;
        }

        // Execute
        world.strikeLightning(damagee.getLocation());
        event.setDamage(event.getDamage() + 4);
    }    
}