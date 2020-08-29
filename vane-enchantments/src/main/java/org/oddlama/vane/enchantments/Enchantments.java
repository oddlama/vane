package org.oddlama.vane.enchantments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_16_R1.IChatBaseComponent;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

@VaneModule(name = "enchantments", bstats = 8640, config_version = 1, lang_version = 1, storage_version = 1)
public class Enchantments extends Module<Enchantments> {
	public Enchantments() {
		try {
			final var accepting = Enchantment.class.getDeclaredField("acceptingNew");
			accepting.setAccessible(true);
			accepting.set(null, true);
		} catch (NoSuchFieldException |	IllegalAccessException e) {
			log.severe("Could not re-enable enchantment registration! Shutting down.");
			getServer().shutdown();
		}

		new org.oddlama.vane.enchantments.commands.Enchant(this);
		new org.oddlama.vane.enchantments.enchantments.Angel(this);
		new org.oddlama.vane.enchantments.enchantments.Careless(this);
		new org.oddlama.vane.enchantments.enchantments.GrapplingHook(this);
		new org.oddlama.vane.enchantments.enchantments.HellBent(this);
		new org.oddlama.vane.enchantments.enchantments.Leafchopper(this);
		new org.oddlama.vane.enchantments.enchantments.Rake(this);
		new org.oddlama.vane.enchantments.enchantments.Seeding(this);
		new org.oddlama.vane.enchantments.enchantments.TakeOff(this);
		new org.oddlama.vane.enchantments.enchantments.Unbreakable(this);
		new org.oddlama.vane.enchantments.enchantments.Wings(this);
	}

	@Override
	public void on_load() {
		// Give custom enchantments a chance to add superseding enchantments
		CustomEnchantment.call_register_superseding();
	}

	public ItemStack update_enchanted_item(ItemStack item_stack) {
		return update_enchanted_item(item_stack, item_stack.getEnchantments());
	}

	public ItemStack update_enchanted_item(ItemStack item_stack, Map<Enchantment, Integer> enchantments) {
		remove_superseded(item_stack, enchantments);
		update_lore(item_stack, enchantments);
		return item_stack;
	}

	private void remove_superseded(ItemStack item_stack, Map<Enchantment, Integer> enchantments) {
		if (enchantments.isEmpty()) {
			return;
		}

		// Track all superseded enchantments
		var superseded = new ArrayList<Enchantment>();

		// All enchantments superseding other enchantments.
		// when b supersedes [a, ..] and c supersedes [b, ..], this algorithm will not
		// remove a when both b and c are added.
		// HINT: circular dependencies (a -> [b, ..], b -> [a, ..] will result neither a nor b being added
		var superseding = new ArrayList<CustomEnchantment<?>>();

		// Get superseded and superseding enchantments
		for (var e : enchantments.keySet()) {
			if (!(e instanceof BukkitEnchantmentWrapper)) {
				continue;
			}

			var custom = ((BukkitEnchantmentWrapper)e).custom_enchantment();
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
		final var lore = new ArrayList<IChatBaseComponent>();
		enchantments.entrySet().stream()
			.filter(p -> p.getKey() instanceof BukkitEnchantmentWrapper)
			.sorted(Map.Entry.<Enchantment, Integer>comparingByKey((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()))
				.thenComparing(Map.Entry.<Enchantment, Integer>comparingByValue()))
			.forEach(p -> {
				lore.add(((BukkitEnchantmentWrapper)p.getKey()).custom_enchantment().display_name(p.getValue()));
			});

		// Set lore
		var meta = item_stack.getItemMeta();
		Nms.set_lore(meta, lore);
		item_stack.setItemMeta(meta);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_prepare_anvil(final PrepareAnvilEvent event) {
		if (event.getResult() == null) {
			return;
		}

		event.setResult(update_enchanted_item(event.getResult().clone()));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_enchant_item(final EnchantItemEvent event) {
		final var map = new HashMap<Enchantment, Integer>(event.getEnchantsToAdd());
		map.putAll(event.getItem().getEnchantments());
		update_enchanted_item(event.getItem(), map);
	}
}
