package org.oddlama.vane.enchantments.enchantments;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.bukkit.loot.LootTables;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.AncientTomeOfTheGods;
import org.oddlama.vane.enchantments.items.BookVariant;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "angel", max_level = 5, rarity = Rarity.VERY_RARE, treasure = true)
public class Angel extends CustomEnchantment<Enchantments> {
	@ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Acceleration percentage. Each tick, the current flying speed is increased X percent towards the target speed. Low values (~0.1) typically result in a smooth acceleration curve and a natural feeling.")
	private double config_acceleration_percentage;
	@ConfigDoubleList(def = {0.7, 1.1, 1.4, 1.7, 2.0}, min = 0.0, desc = "Flying speed in blocks per second for each enchantment level.")
	private List<Double> config_speed;

	public Angel(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void register_superseding() {
		supersedes(bukkit(Unbreakable.class));
	}

	@Override
	public void register_recipes() {
		final var ancient_tome_of_the_gods_enchanted = CustomItem.<AncientTomeOfTheGods.AncientTomeOfTheGodsVariant>variant_of(AncientTomeOfTheGods.class, BookVariant.ENCHANTED_BOOK).item();
		final var ancient_tome_of_the_gods = CustomItem.<AncientTomeOfTheGods.AncientTomeOfTheGodsVariant>variant_of(AncientTomeOfTheGods.class, BookVariant.BOOK).item();

		final var recipe_key = recipe_key();
		final var item = ancient_tome_of_the_gods_enchanted.clone();
		final var meta = (EnchantmentStorageMeta)item.getItemMeta();
		meta.addStoredEnchant(bukkit(), 1, false);
		item.setItemMeta(meta);
		get_module().update_enchanted_item(item);

		final var recipe = new ShapedRecipe(recipe_key, item)
			.shape("prp",
				   "mbm",
				   "mdm")
			.setIngredient('b', ancient_tome_of_the_gods)
			.setIngredient('m', Material.PHANTOM_MEMBRANE)
			.setIngredient('d', Material.DRAGON_BREATH)
			.setIngredient('p', Material.PUFFERFISH_BUCKET)
			.setIngredient('r', Material.FIREWORK_ROCKET);

		add_recipe(recipe);

		// Loot generation
		final var entry = new LootTableEntry(250, item);
		for (final var table : new LootTables[] {
			LootTables.BURIED_TREASURE,
			LootTables.PILLAGER_OUTPOST,
			LootTables.RUINED_PORTAL,
			LootTables.STRONGHOLD_LIBRARY,
			LootTables.UNDERWATER_RUIN_BIG,
			LootTables.VILLAGE_TEMPLE,
		}) {
			get_module().loot_table(table).put(recipe_key, entry);
		}
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return item_stack.getType() == Material.ELYTRA;
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
		loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 0, -new_vel.getX(), -new_vel.getY(), -new_vel.getZ(), 0.4);
	}
}
