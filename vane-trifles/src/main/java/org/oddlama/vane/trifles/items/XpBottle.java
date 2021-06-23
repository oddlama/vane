package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.give_item;
import static org.oddlama.vane.util.PlayerUtil.remove_one_item_from_hand;
import static org.oddlama.vane.util.Util.exp_for_level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "xp_bottle")
public class XpBottle extends CustomItem<Trifles, XpBottle> {
	public static enum Variant implements ItemVariantEnum {
		SMALL,
		MEDIUM,
		LARGE;

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return true; }
	}

	public static class XpBottleVariant extends CustomItemVariant<Trifles, XpBottle, Variant> {
		@ConfigInt(def = -1, desc = "Level capacity.")
		public int config_capacity;

		public XpBottleVariant(XpBottle parent, Variant variant) {
			super(parent, variant);
		}

		@Override
		public Material base() {
			return Material.HONEY_BOTTLE;
		}

		@Override
		public Component display_name() {
			return super.display_name()
				.color(NamedTextColor.YELLOW);
		}

		public int config_capacity_def() {
			switch (variant()) {
				default:     throw new RuntimeException("Missing variant case. This is a bug.");
				case SMALL:  return 10;
				case MEDIUM: return 20;
				case LARGE:  return 30;
			}
		}
	}

	public XpBottle(Context<Trifles> context) {
		super(context, Variant.class, Variant.values(), XpBottleVariant::new);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_player_item_consume(final PlayerItemConsumeEvent event) {
		final var player = event.getPlayer();

		// Get item variant
		final var item = event.getItem();
		final var variant = this.<XpBottleVariant>variant_of(item);
		if (variant == null || !variant.enabled()) {
			return;
		}

		// Get empty bottle variant
		final var empty_variant = CustomItem.SingleVariant.SINGLETON;
		final var empty_xp_bottle_variant = CustomItem.<EmptyXpBottle.EmptyXpBottleVariant>variant_of(EmptyXpBottle.class, empty_variant);

		// Exchange items
		final var main_hand = item.equals(player.getInventory().getItemInMainHand());
		remove_one_item_from_hand(player, main_hand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
		give_item(player, empty_xp_bottle_variant.item());

		// Add player experience without applying mending effects
		get_module().last_xp_bottle_consume_time.put(player.getUniqueId(), System.currentTimeMillis());
		player.giveExp(exp_for_level(variant.config_capacity), false);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

		// Do not consume actual base item
		event.setCancelled(true);
	}
}
