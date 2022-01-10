package org.oddlama.vane.enchantments.enchantments;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.LootTable.LootTableEntry;
import org.oddlama.vane.core.data.CooldownData;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.enchantments.items.AncientTomeOfTheGods;
import org.oddlama.vane.enchantments.items.BookVariant;

@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allow_custom = true)
public class Soulbound extends CustomEnchantment<Enchantments> {

	@ConfigLong(
		def = 2000,
		min = 0,
		desc = "Window to allow Soulbound item drop immediately after a previous drop in milliseconds"
	)
	public long config_cooldown;

	private static final NamespacedKey IGNORE_SOULBOUND_DROP = namespaced_key(
		"vane_enchantments",
		"ignore_soulbound_drop"
	);
	private CooldownData drop_cooldown = new CooldownData(IGNORE_SOULBOUND_DROP, config_cooldown);

	@LangMessage
	public TranslatedMessage lang_drop_lock_warning;

	@LangMessage
	public TranslatedMessage lang_dropped_notification;

	@LangMessage
	public TranslatedMessage lang_drop_cooldown;

	public Soulbound(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public void on_config_change() {
		super.on_config_change();
		drop_cooldown = new CooldownData(IGNORE_SOULBOUND_DROP, config_cooldown);
	}

	@Override
	public void register_recipes() {
		final var ancient_tome_of_the_gods_enchanted = CustomItem
			.<AncientTomeOfTheGods.AncientTomeOfTheGodsVariant>variant_of(
				AncientTomeOfTheGods.class,
				BookVariant.ENCHANTED_BOOK
			)
			.item();
		final var ancient_tome_of_the_gods = CustomItem
			.<AncientTomeOfTheGods.AncientTomeOfTheGodsVariant>variant_of(AncientTomeOfTheGods.class, BookVariant.BOOK)
			.item();

		final var recipe_key = recipe_key();
		final var item = ancient_tome_of_the_gods_enchanted.clone();
		final var meta = (EnchantmentStorageMeta) item.getItemMeta();
		meta.addStoredEnchant(bukkit(), 1, false);
		item.setItemMeta(meta);
		get_module().update_enchanted_item(item);

		final var curse_of_binding = new ItemStack(Material.ENCHANTED_BOOK);
		final var curse_meta = (EnchantmentStorageMeta) curse_of_binding.getItemMeta();
		curse_meta.addStoredEnchant(Enchantment.BINDING_CURSE, 1, false);
		curse_of_binding.setItemMeta(curse_meta);

		final var recipe = new ShapedRecipe(recipe_key, item)
			.shape("cqc", "obe", "rgt")
			.setIngredient('b', ancient_tome_of_the_gods)
			.setIngredient('c', Material.CHAIN)
			.setIngredient('q', Material.WRITABLE_BOOK)
			.setIngredient('o', Material.BONE)
			.setIngredient('r', curse_of_binding)
			.setIngredient('g', Material.GHAST_TEAR)
			.setIngredient('t', Material.TOTEM_OF_UNDYING)
			.setIngredient('e', Material.ENDER_EYE);

		add_recipe(recipe);

		// Alternate recipe with empty lore (for backwards compatibility)
		final var curse_of_binding_empty_lore = new ItemStack(Material.ENCHANTED_BOOK);
		final var curse_meta_empty_lore = (EnchantmentStorageMeta) curse_of_binding_empty_lore.getItemMeta();
		curse_meta_empty_lore.addStoredEnchant(Enchantment.BINDING_CURSE, 1, false);
		curse_meta_empty_lore.lore(new ArrayList<Component>());
		curse_of_binding_empty_lore.setItemMeta(curse_meta_empty_lore);

		final var recipe_key_empty_lore = recipe_key("empty_lore");
		final var recipe_empty_lore = new ShapedRecipe(recipe_key_empty_lore, item)
			.shape("cqc", "obe", "rgt")
			.setIngredient('b', ancient_tome_of_the_gods)
			.setIngredient('c', Material.CHAIN)
			.setIngredient('q', Material.WRITABLE_BOOK)
			.setIngredient('o', Material.BONE)
			.setIngredient('r', curse_of_binding_empty_lore)
			.setIngredient('g', Material.GHAST_TEAR)
			.setIngredient('t', Material.TOTEM_OF_UNDYING)
			.setIngredient('e', Material.ENDER_EYE);

		add_recipe(recipe_empty_lore);

		// Loot generation
		get_module().loot_table(LootTables.BASTION_TREASURE).put(recipe_key, new LootTableEntry(15, item));
	}

	@Override
	public Component apply_display_format(Component component) {
		return component.color(NamedTextColor.DARK_GRAY);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_death(final PlayerDeathEvent event) {
		final var keep_items = event.getItemsToKeep();

		// Keep all soulbound items
		final var it = event.getDrops().iterator();
		while (it.hasNext()) {
			final var drop = it.next();
			if (is_soulbound(drop)) {
				keep_items.add(drop);
				it.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_player_inventory_check(final InventoryClickEvent event) {
		if (event.getCursor() == null) return;
		if (!is_soulbound(event.getCursor())) return;
		if (
			event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR
		) {
			boolean too_slow = drop_cooldown.peek_cooldown(event.getCursor().getItemMeta());
			if (too_slow) {
				// Dropped too slow, refresh and cancel
				final ItemMeta meta = event.getCursor().getItemMeta();
				drop_cooldown.check_or_update_cooldown(meta);
				event.getCursor().setItemMeta(meta);
				lang_drop_cooldown.send_action_bar(event.getWhoClicked());
				event.setResult(Event.Result.DENY);
				return;
			}
			return;
			// else allow as normal
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_drop_item(final PlayerDropItemEvent event) {
		// Soulbound items cannot be dropped by a player.
		// Prevents yeeting your best sword out of existence.
		// (It's okay to put them into chests)
		final var dropped_item = event.getItemDrop().getItemStack();
		if (is_soulbound(dropped_item)) {
			boolean too_slow = drop_cooldown.peek_cooldown(dropped_item.getItemMeta());
			if (!too_slow) {
				var meta = dropped_item.getItemMeta();
				drop_cooldown.clear(dropped_item.getItemMeta());
				dropped_item.setItemMeta(meta);
				lang_dropped_notification.send(event.getPlayer(), dropped_item.displayName());
				return;
			}
			final var inventory = event.getPlayer().getInventory();
			if (inventory.firstEmpty() != -1) {
				// We still have space in the inventory, so the player tried to drop it with Q.
				event.setCancelled(true);
				lang_drop_lock_warning.send_action_bar(event.getPlayer(), event.getItemDrop().getItemStack().displayName());
			} else {
				// Inventory is full (e.g. when exiting crafting table with soulbound item in it)
				// so we drop the first non-soulbound item (if any) instead.
				final var it = inventory.iterator();
				ItemStack non_soulbound_item = null;
				int non_soulbound_item_slot = 0;
				while (it.hasNext()) {
					final var item = it.next();
					if (item.getEnchantmentLevel(this.bukkit()) == 0) {
						non_soulbound_item = item;
						break;
					}

					++non_soulbound_item_slot;
				}

				if (non_soulbound_item == null) {
					// We can't prevent dropping a soulbound item.
					// Well that sucks.
					return;
				}

				// Drop the other item
				final var player = event.getPlayer();
				inventory.setItem(non_soulbound_item_slot, dropped_item);
				player.getLocation().getWorld().dropItem(player.getLocation(), non_soulbound_item);
				lang_drop_lock_warning.send_action_bar(player, event.getItemDrop().getItemStack().displayName());
				event.setCancelled(true);
			}
		}
	}

	private boolean is_soulbound(ItemStack dropped_item) {
		return dropped_item.getEnchantmentLevel(this.bukkit()) > 0;
	}
}
