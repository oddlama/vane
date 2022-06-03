package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.ItemUtil.damage_item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent; //shouldnt need this
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;
import org.bukkit.WeatherType;
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

@VaneEnchantment(name = "lightning", max_level = max_level = 1, rarity = rarity.RARE, treasure = true, allow_custom = true)
public class Ligtning extends CustomEnchantment<Enchantments> {

    // @ConfigIntList(
    //     def = ,
    //     min = 0,
    //     desc = "Boost cooldown in milliseconds for each enchantment level."
    // )

    public Ligtning(Context<Enchantments> context) {
        super(context);
    }

    @Override
    public RecipeList default_recipes() {
        return RecipeList.of(new SHapedRecipeDefinition("generic")
        .shape("r r","utu"," b ")
        .set_ingredient('r', Material.LIGHTNING_ROD)
        .set_ingredient('t', "vane_enchantments:anchient_tome_of_knowledge")
        .set_ingredient('b', Material.BEACON)
        .set_ingredient('u', Material.TOTEM_OF_UNDYING)
        .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"));
        )
    }

    //Do I need loot tables? Does that add it to the wild?

    @Override
    public boolean can_enchant(@NotNull ItemStack item_stack) {
        return item_stack.getType() == Material.sword;
    }

    @EventHandler(priority = EvnetPriority.NORMAL, ignoreCancelled = true)
    //on damage event
    public void EntityDamageByEntityEvent(Entity damager, Entity damagee, EntityDamageEvent.DamageCause cause, double damage) {
    //test if it is storming or raining
        if(WeatherType.valueOf() == "DOWNFALL"){
            
            //test if the user can see the sky
            if(damager.getLocation().getBlockY() < damager.getWorld().getHighestBlockYAt(damager.getLocation())){

                //Add a cooldown? 5s-10s?

                //hit the target with lightning
                world.strikeLightning(damagee.getLocation);
            }
        }
    }    
}