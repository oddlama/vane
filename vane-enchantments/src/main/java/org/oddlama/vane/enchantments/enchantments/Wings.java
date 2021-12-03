package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.apply_elytra_boost;
import static org.oddlama.vane.util.Util.ms_to_ticks;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.BookVariant;

@VaneEnchantment(name = "wings", max_level = 4, rarity = Rarity.RARE, treasure = true, allow_custom = true)
public class Wings extends CustomEnchantment<Enchantments> {

	@ConfigIntList(
		def = { 7000, 5000, 3500, 2800 },
		min = 0,
		desc = "Boost cooldown in milliseconds for each enchantment level."
	)
	private List<Integer> config_boost_cooldowns;

	@ConfigDoubleList(def = { 0.4, 0.47, 0.54, 0.6 }, min = 0.0, desc = "Boost strength for each enchantment level.")
	private List<Double> config_boost_strengths;

	public Wings(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void register_recipes() {
		final var ancient_tome_of_knowledge_enchanted = CustomItem
			.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(
				AncientTomeOfKnowledge.class,
				BookVariant.ENCHANTED_BOOK
			)
			.item();
		final var ancient_tome_of_knowledge = CustomItem
			.<AncientTomeOfKnowledge.AncientTomeOfKnowledgeVariant>variant_of(
				AncientTomeOfKnowledge.class,
				BookVariant.BOOK
			)
			.item();

		final var recipe_key = recipe_key();
		final var item = ancient_tome_of_knowledge_enchanted.clone();
		final var meta = (EnchantmentStorageMeta) item.getItemMeta();
		meta.addStoredEnchant(bukkit(), 1, false);
		item.setItemMeta(meta);
		get_module().update_enchanted_item(item);

		final var recipe = new ShapedRecipe(recipe_key, item)
			.shape("m m", "dbd", "r r")
			.setIngredient('b', ancient_tome_of_knowledge)
			.setIngredient('m', Material.PHANTOM_MEMBRANE)
			.setIngredient('d', Material.DISPENSER)
			.setIngredient('r', Material.FIREWORK_ROCKET);

		add_recipe(recipe);

		// Loot generation
		final var entry = new LootTableEntry(110, item);
		for (final var table : new LootTables[] {
			LootTables.BURIED_TREASURE,
			LootTables.PILLAGER_OUTPOST,
			LootTables.RUINED_PORTAL,
			LootTables.SHIPWRECK_TREASURE,
			LootTables.STRONGHOLD_LIBRARY,
			LootTables.UNDERWATER_RUIN_BIG,
			LootTables.UNDERWATER_RUIN_SMALL,
			LootTables.VILLAGE_TEMPLE,
			LootTables.WOODLAND_MANSION,
		}) {
			get_module().loot_table(table).put(recipe_key, entry);
		}

		get_module().loot_table(LootTables.BASTION_TREASURE).put(recipe_key, new LootTableEntry(10, item));
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return item_stack.getType() == Material.ELYTRA;
	}

	private int get_boost_cooldown(int level) {
		if (level > 0 && level <= config_boost_cooldowns.size()) {
			return config_boost_cooldowns.get(level - 1);
		}
		return config_boost_cooldowns.get(0);
	}

	private double get_boost_strength(int level) {
		if (level > 0 && level <= config_boost_strengths.size()) {
			return config_boost_strengths.get(level - 1);
		}
		return config_boost_strengths.get(0);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_toggle_sneak(PlayerToggleSneakEvent event) {
		// Check sneaking and flying
		final var player = event.getPlayer();
		if (!event.isSneaking() || !player.isGliding()) {
			return;
		}

		// Check enchantment level
		final var chest = player.getEquipment().getChestplate();
		final var level = chest.getEnchantmentLevel(this.bukkit());
		if (level == 0) {
			return;
		}

		// Check cooldown
		if (player.getCooldown(Material.ELYTRA) > 0) {
			return;
		}

		// Apply boost
		final var cooldown = ms_to_ticks(get_boost_cooldown(level));
		player.setCooldown(Material.ELYTRA, (int) cooldown);
		apply_elytra_boost(player, get_boost_strength(level));
		damage_item(player, chest, (int) (1.0 + 2.0 * Math.random()));

		// Spawn particles
		final var loc = player.getLocation();
		final var vel = player.getVelocity().length();
		for (int i = 0; i < 16; ++i) {
			final var rnd = Vector.getRandom().subtract(new Vector(.5, .5, .5)).normalize().multiply(.25);
			final var dir = rnd.clone().multiply(.5).subtract(player.getVelocity());
			loc
				.getWorld()
				.spawnParticle(
					Particle.FIREWORKS_SPARK,
					loc.add(rnd),
					0,
					dir.getX(),
					dir.getY(),
					dir.getZ(),
					vel * ThreadLocalRandom.current().nextDouble(0.4, 0.6)
				);
		}
	}
}
