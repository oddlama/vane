package org.oddlama.vane.enchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

@VaneModule(name = "enchantments", bstats = 8640, config_version = 1, lang_version = 3, storage_version = 1)
public class Enchantments extends Module<Enchantments> {
	private static final NamespacedKey SENTINEL = Util.namespaced_key("vane", "enchantment_lore");

	public Enchantments() {
		try {
			final var accepting = Enchantment.class.getDeclaredField("acceptingNew");
			accepting.setAccessible(true);
			accepting.set(null, true);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.severe("Could not re-enable enchantment registration! Shutting down.");
			getServer().shutdown();
		}

		new org.oddlama.vane.enchantments.items.Tomes(this);

		new org.oddlama.vane.enchantments.commands.Enchant(this);
		new org.oddlama.vane.enchantments.enchantments.Angel(this);
		new org.oddlama.vane.enchantments.enchantments.Careless(this);
		new org.oddlama.vane.enchantments.enchantments.GrapplingHook(this);
		new org.oddlama.vane.enchantments.enchantments.HellBent(this);
		new org.oddlama.vane.enchantments.enchantments.Leafchopper(this);
		new org.oddlama.vane.enchantments.enchantments.Rake(this);
		new org.oddlama.vane.enchantments.enchantments.Seeding(this);
		new org.oddlama.vane.enchantments.enchantments.Soulbound(this);
		new org.oddlama.vane.enchantments.enchantments.TakeOff(this);
		new org.oddlama.vane.enchantments.enchantments.Unbreakable(this);
		new org.oddlama.vane.enchantments.enchantments.Wings(this);
	}

	@Override
	public void on_load() {
		// Give custom enchantments a chance to add superseding enchantments
		CustomEnchantment.call_register_superseding();
		// TODO this is temporary, until the enchantment API moves to core.
		core.item_stack_enchantment_updaters.add(this::update_enchanted_item);
	}

	public ItemStack update_enchanted_item(ItemStack item_stack) {
		return update_enchanted_item(item_stack, new HashMap<Enchantment, Integer>(), false);
	}

	public ItemStack update_enchanted_item(ItemStack item_stack, Map<Enchantment, Integer> additional_enchantments) {
		return update_enchanted_item(item_stack, additional_enchantments, false);
	}

	public ItemStack update_enchanted_item(ItemStack item_stack, boolean only_if_enchanted) {
		return update_enchanted_item(item_stack, new HashMap<Enchantment, Integer>(), only_if_enchanted);
	}

	public ItemStack update_enchanted_item(
		ItemStack item_stack,
		Map<Enchantment, Integer> additional_enchantments,
		boolean only_if_enchanted
	) {
		final var enchantments = new HashMap<>(additional_enchantments);
		final var meta = item_stack.getItemMeta();
		if (meta instanceof EnchantmentStorageMeta) {
			enchantments.putAll(((EnchantmentStorageMeta) meta).getStoredEnchants());
		} else {
			enchantments.putAll(item_stack.getEnchantments());
		}

		if (enchantments.isEmpty() && only_if_enchanted) {
			return item_stack;
		}

		remove_superseded(item_stack, enchantments);
		update_lore(item_stack, enchantments);
		return item_stack;
	}

	private void remove_superseded(ItemStack item_stack, Map<Enchantment, Integer> enchantments) {
		if (enchantments.isEmpty()) {
			return;
		}

		// Track all superseded enchantments
		final var superseded = new ArrayList<Enchantment>();

		// All enchantments superseding other enchantments.
		// when b supersedes [a, ..] and c supersedes [b, ..], this algorithm will not
		// remove a when both b and c are added.
		// HINT: circular dependencies (a -> [b, ..], b -> [a, ..] will result neither a nor b being added
		final var superseding = new ArrayList<CustomEnchantment<?>>();

		// Get superseded and superseding enchantments
		for (final var e : enchantments.keySet()) {
			if (!(e instanceof BukkitEnchantmentWrapper)) {
				continue;
			}

			final var custom = ((BukkitEnchantmentWrapper) e).custom_enchantment();
			if (!custom.supersedes().isEmpty()) {
				superseding.add(custom);
				superseded.addAll(custom.supersedes());
			}
		}

		// Remove all enchantments that supersede others but got superseded themselves
		superseding.removeAll(superseded);

		// Rebuild list with superseded enchantments. Now superseded will have no
		// element in common with superseding
		superseded.clear();
		for (var e : superseding) {
			superseded.addAll(e.supersedes());
		}

		// Retain only the remaining enchantments
		for (var e : superseded) {
			item_stack.removeEnchantment(e);
		}
	}

	private void update_lore(ItemStack item_stack, Map<Enchantment, Integer> enchantments) {
		// Create lore by converting enchantment name and level to string
		// and prepend rarity color (can be overwritten in description)
		final var vane_enchantments = enchantments
			.entrySet()
			.stream()
			.filter(p -> p.getKey() instanceof BukkitEnchantmentWrapper)
			.sorted(Map.Entry.<Enchantment, Integer>comparingByKey(Comparator.comparing(x -> x.getKey().toString()))
				.thenComparing(Map.Entry.comparingByValue())
			)
			.toList();

		var lore = item_stack.lore();
		if (lore == null) {
			lore = new ArrayList<Component>();
		}

		lore.removeIf(this::is_enchantment_lore);
		lore.addAll(0, vane_enchantments.stream().map(ench ->
			ItemUtil.add_sentinel(((BukkitEnchantmentWrapper) ench.getKey()).custom_enchantment().display_name(ench.getValue()), SENTINEL)
		).toList());

		// Set lore
		item_stack.lore(lore.isEmpty() ? null : lore);
	}

	private boolean is_enchantment_lore(final Component component) {
		// If the component begins with a translated lore from vane enchantments, it is always from us. (needed for backward compatibility)
		if (component instanceof TranslatableComponent && ((TranslatableComponent)component).key().startsWith(namespace() + ".")) {
			return true;
		}

		return ItemUtil.has_sentinel(component, SENTINEL);
	}

	// Triggers on Anvils, grindstones, and smithing tables.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_prepare_enchanted_edit(final PrepareResultEvent event) {
		if (event.getResult() == null) {
			return;
		}

		event.setResult(update_enchanted_item(event.getResult().clone()));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_enchant_item(final EnchantItemEvent event) {
		final var map = new HashMap<Enchantment, Integer>(event.getEnchantsToAdd());
		update_enchanted_item(event.getItem(), map);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_loot_generate(final LootGenerateEvent event) {
		for (final var item : event.getLoot()) {
			// Update all item lore in case they are enchanted
			update_enchanted_item(item, true);
		}
	}

	private MerchantRecipe process_recipe(final MerchantRecipe recipe) {
		var result = recipe.getResult().clone();

		// Create new recipe
		final var new_recipe = new MerchantRecipe(
			update_enchanted_item(result, true),
			recipe.getUses(),
			recipe.getMaxUses(),
			recipe.hasExperienceReward(),
			recipe.getVillagerExperience(),
			recipe.getPriceMultiplier()
		);
		recipe.getIngredients().forEach(i -> new_recipe.addIngredient(i));
		return new_recipe;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_acquire_trade(final VillagerAcquireTradeEvent event) {
		event.setRecipe(process_recipe(event.getRecipe()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_right_click_villager(final PlayerInteractEntityEvent event) {
		final var entity = event.getRightClicked();
		if (!(entity instanceof Merchant)) {
			return;
		}

		final var merchant = (Merchant) entity;
		final var recipes = new ArrayList<MerchantRecipe>();

		// Check all recipes
		for (final var r : merchant.getRecipes()) {
			recipes.add(process_recipe(r));
		}

		// Update recipes
		merchant.setRecipes(recipes);
	}
}
