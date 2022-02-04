package org.oddlama.vane.enchantments;

import static net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
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
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.ModelDataEnum;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "enchantments", bstats = 8640, config_version = 1, lang_version = 2, storage_version = 1)
public class Enchantments extends Module<Enchantments> {

	public Enchantments() {
		try {
			final var accepting = Enchantment.class.getDeclaredField("acceptingNew");
			accepting.setAccessible(true);
			accepting.set(null, true);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.severe("Could not re-enable enchantment registration! Shutting down.");
			getServer().shutdown();
		}

		new org.oddlama.vane.enchantments.items.AncientTome(this);
		new org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge(this);
		new org.oddlama.vane.enchantments.items.AncientTomeOfTheGods(this);

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
	}

	@Override
	public Class<? extends ModelDataEnum> model_data_enum() {
		return org.oddlama.vane.enchantments.items.ModelData.class;
	}

	@Override
	public int model_data(int item_id, int variant_id) {
		return Core.model_data(1, item_id, variant_id);
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
			.sorted(
				Map.Entry
					.<Enchantment, Integer>comparingByKey(Comparator.comparing(x -> x.getKey().toString()))
					.thenComparing(Map.Entry.comparingByValue())
			)
			.toList();

		var lore = item_stack.lore();
		if (lore == null) lore = new ArrayList<>();

		lore.removeIf(this::is_enchantment_lore);
		lore.addAll(0, vane_enchantments.stream().map(this::lore_for_enchantment).toList());

		// Set lore
		final var meta = item_stack.getItemMeta();
		meta.lore(lore.isEmpty() ? null : lore);
		item_stack.setItemMeta(meta);
	}

	private final TextComponent SENTINEL_VALUE = Component.text(namespace() + ":lore");

	private boolean is_enchantment_lore(final Component component) {
		// If the component begins with a translated lore from vane enchantments, it is always from us. (needed for backward compatibility)
		if (
			component instanceof TranslatableComponent &&
			((TranslatableComponent) component).key().startsWith(namespace() + ".")
		) {
			return true;
		}

		// Whether the lore line is prefixed with a sentinel value marking this lore line as a vane-enchantment owned lore.
		final HoverEvent<?> hover = component.hoverEvent();
		if (hover == null) return false;
		final var hoverValue = hover.value();
		if (hoverValue instanceof TextComponent) {
			return (
				hover.action() == SHOW_TEXT && SENTINEL_VALUE.content().equals(((TextComponent) hoverValue).content())
			);
		}
		return false;
	}

	private Component lore_for_enchantment(final Map.Entry<Enchantment, Integer> ench) {
		var standard = ((BukkitEnchantmentWrapper) ench.getKey()).custom_enchantment().display_name(ench.getValue());
		return standard.hoverEvent(HoverEvent.showText(SENTINEL_VALUE));
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
