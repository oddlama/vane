package org.oddlama.vane.core.data;

import static org.oddlama.vane.util.ItemUtil.damage_item;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.util.Util;

public class DurabilityOverrideData {
	private static final PersistentDataType<Integer, Integer> type_max = PersistentDataType.INTEGER;
	private static final PersistentDataType<Integer, Integer> type_damage = PersistentDataType.INTEGER;

	private final NamespacedKey key_max;
	private final NamespacedKey key_damage;
	private final int max_durability;
	@Nullable private final TranslatableComponent tooltip;

	private static final Style DEFAULT_TOOLTIP_STYLE = Style
		.style(NamedTextColor.WHITE)
		.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
	private static final TranslatableComponent DEFAULT_TOOLTIP = Component
		.translatable("item.durability")
		.style(DEFAULT_TOOLTIP_STYLE);

	public DurabilityOverrideData(int max_durability) {
		this(
			max_durability,
			DEFAULT_TOOLTIP
		);
	}

	public DurabilityOverrideData(int max_durability, @Nullable TranslatableComponent tooltip) {
		this(
			max_durability,
			tooltip,
			Util.namespaced_key("vane", "durability.max"),
			Util.namespaced_key("vane", "durability.damage")
		);
	}

	public DurabilityOverrideData(int max_durability, @Nullable TranslatableComponent tooltip, NamespacedKey key_max, NamespacedKey key_damage) {
		this.key_max = key_max;
		this.tooltip = tooltip;
		this.key_damage = key_damage;
		this.max_durability = max_durability;
	}

	public int max(final ItemStack stack) {
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(key_max, type_max, max_durability);
	}

	public void max(final ItemStack stack, int max) {
		modifying(stack, pdc -> pdc.set(key_max, type_max, max));
	}

	public int damage(final ItemStack stack) {
		Damageable dmg = stack.getItemMeta() instanceof Damageable ? (Damageable) stack.getItemMeta() : null;
		var default_value = dmg == null ? 0 : dmg.getDamage();
		return stack.getItemMeta().getPersistentDataContainer().getOrDefault(key_damage, type_damage, default_value);
	}

	public void damage(final ItemStack stack, int current) {
		modifying(stack, pdc -> pdc.set(key_damage, type_damage, current));
	}

	private static void modifying(ItemStack stack, Consumer<PersistentDataContainer> worker) {
		var meta = stack.getItemMeta();
		var pdc = meta.getPersistentDataContainer();
		worker.accept(pdc);
		stack.setItemMeta(meta);
	}

	/**
	 * *Always* updates the item's fake durability lore + data values.
	 * forces the player to use the item, potentially causing damage if enchantments allow.
	 * @return if the item was damaged.
	 */
	public boolean use_item(final Player player, final ItemStack stack) {
		final ItemMeta itemMeta = stack.getItemMeta();
		if (!(itemMeta instanceof Damageable)) return false;

		var real_global_max = stack.getType().getMaxDurability();
		var before_damage = ((Damageable) stack.getItemMeta()).getDamage();
		var fake_individual_max = this.max(stack);
		var fake_damage = this.damage(stack);

		damage_item(player, stack, 1);

		Integer after_damage = null;
		if ((stack.getItemMeta() instanceof Damageable)) {
			after_damage = ((Damageable) stack.getItemMeta()).getDamage();
		}
		if (after_damage == null) return true; //Thing broke.

		var diff = after_damage - before_damage;
		// track the 'real' dmg
		int new_damage = fake_damage + diff;
		damage(stack, new_damage);
		var technically_correct_percent = ((float)new_damage) / fake_individual_max;
		float dangerous_interpolated_damage = technically_correct_percent * real_global_max;
		// We need to clamp it above 0, so the item doesn't randomly break.
		var safe_damage = damage_clamp(new_damage, fake_individual_max, dangerous_interpolated_damage, real_global_max);
		final Damageable itemMeta1 = ((Damageable) stack.getItemMeta());
		itemMeta1.setDamage(safe_damage);
		update_lore(itemMeta1, new_damage, fake_individual_max);
		stack.setItemMeta(itemMeta1);
		return diff != 0;
	}

	private void update_lore(ItemMeta itemMeta1, int dmg, int max) {
		var lore = itemMeta1.lore();
		final boolean found_lore = lore != null && lore.stream().anyMatch(this::matches);

		List<Component> new_lore = lore;
		if (new_lore == null) new_lore = Lists.newArrayList();

		if (found_lore) {
			new_lore = lore.stream()
				.flatMap(l -> {
					if (matches(l)) {
						if (itemMeta1.isUnbreakable() || tooltip == null) return Stream.empty();
						// Stop those pesky overshoots looking so derpy.
						// only possible with desyncs / config changes anyway.
						final var uses_remaining = Math.max(1, Math.min(max - dmg, max));
						return Stream.of(tooltip_lore(uses_remaining, max));
					} else {
						return Stream.of(l);
					}
				})
				.toList();
		} else if (tooltip != null) {
			new_lore.add(tooltip_lore(max - dmg, max));
		}
		itemMeta1.lore(new_lore);
	}

	protected Component tooltip_lore(int uses_remaining, int max) {
		if (tooltip == null) return null;
		return tooltip.args(Component.text(uses_remaining), Component.text(max));
	}

	protected boolean matches(Component a) {
		if (tooltip == null) return false;
		if (!(a instanceof TranslatableComponent a_as_TC)) return false;
		return tooltip.key().equals(a_as_TC.key());
	}

	/**
	 * clamp the display value on the following rules.
	 * IFF and only if there is 1 durability remaining, the output is will reflect that (max-1 damage).
	 * IFF the item is unused, the output is 0.
	 * If the item is consumed, the output is max.
	 * This is to support better emulation of modded client side warnings or datapacks if the tool is about to break etc.
	 */
	private int damage_clamp(int fake_damage, int fake_max, float dangerous_interpolated_damage, short max) {
		// This can only happen in weird edgecases, in which case we recover by breaking the item the next time
		// the player uses it. Much better then showing negative values in the fake durability lore.
		if (fake_damage >= fake_max) return max - 1;
		if (fake_damage == fake_max) return max;
		if (fake_damage == fake_max - 1) return max - 1;
		if (fake_damage == 0) return 0;
		return rounded_clamp(dangerous_interpolated_damage, 1, max - 2);
	}

	private static int rounded_clamp(float f, int min, int max) {
		var out = Math.round(f);
		return Math.min(max, Math.max(min, out));
	}

	public void clear(final ItemStack itemStack) {
		modifying(
			itemStack,
			pdc -> {
				pdc.remove(key_damage);
				pdc.remove(key_max);
			}
		);
		var lore = itemStack.lore();
		if (lore != null) lore.remove(tooltip);
		itemStack.lore(lore);
	}

	public void on_mend(PlayerItemMendEvent mend) {
		// TODO: Durability Overridden items can not yet support mending effectively.
		// see: https://github.com/PaperMC/Paper/issues/7313
		mend.setCancelled(true);
	}
}
