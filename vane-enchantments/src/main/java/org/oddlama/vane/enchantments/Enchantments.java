package org.oddlama.vane.enchantments;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.Enchantment;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.util.Nms;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

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

		new org.oddlama.vane.enchantments.enchantments.Rake(this);
	}

	//public String getRarityColor(net.minecraft.server.v1_14_R1.Enchantment enchantment) {
	//	switch (enchantment.d()) {
	//		case COMMON:
	//		case ORDINARY:
	//		case UNCOMMON:
	//		case ODD:
	//		case RARE:
	//		case SPECIAL:
	//		case VERY_RARE:
	//			return "§r§7";

	//		case HEROIC:
	//			return "§r§2";

	//		case EPIC:
	//			return "§r§3";

	//		case MYTHICAL:
	//			return "§r§6";

	//		case LEGENDARY:
	//			return "§r§5§l";

	//		default:
	//			return "§r§7";
	//	}
	//}

	public ItemStack update_enchanted_item(ItemStack item_stack) {
		remove_superseded(item_stack);
		update_lore(item_stack);
		return item_stack;
	}

	public void remove_superseded(ItemStack item_stack) {
		if (item_stack.getEnchantments().isEmpty()) {
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
		for (var e : item_stack.getEnchantments().keySet()) {
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

	public void update_lore(ItemStack item_stack) {
		// Create lore by converting enchantment name and level to string
		// and prepend rarity color (can be overwritten in description)
		final var lore = new ArrayList<IChatBaseComponent>();
		item_stack.getEnchantments().entrySet().stream()
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
}
