package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.BookVariant;

@VaneEnchantment(name = "hell_bent", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.ARMOR_HEAD)
public class HellBent extends CustomEnchantment<Enchantments> {

	public HellBent(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("m", "b", "t")
			.set_ingredient('b', "vane_enchantments:ancient_tome_of_knowledge")
			.set_ingredient('t', Material.TURTLE_HELMET)
			.set_ingredient('m', Material.MUSIC_DISC_PIGSTEP)
			.result("vane_enchantments:enchanted_ancient_tome_of_knowledge"));
	}
		// Loot generation
		final var entry = new LootTableEntry(50, item);
			LootTables.BASTION_BRIDGE,
			LootTables.BASTION_HOGLIN_STABLE,
			LootTables.BASTION_OTHER,
			LootTables.BASTION_TREASURE,

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_damage(final EntityDamageEvent event) {
		final var entity = event.getEntity();
		if (!(entity instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
			return;
		}

		// Get helmet
		final var player = (Player) entity;
		final var helmet = player.getEquipment().getHelmet();
		if (helmet == null) {
			return;
		}

		// Check enchantment
		if (helmet.getEnchantmentLevel(this.bukkit()) == 0) {
			return;
		}

		event.setCancelled(true);
	}
}
