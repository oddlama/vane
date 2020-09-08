package org.oddlama.vane.core.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Menu {
	private final Inventory inventory;
	private final Set<MenuWidget> widgets = new HashSet<>();

	public Menu(final Inventory inventory) {
		this.inventory = inventory;
	}

	public Inventory inventory() { return inventory; }

	public void add(final MenuWidget widget) {
		widgets.add(widget);
	}

	public boolean remove(final MenuWidget widget) {
		return widgets.remove(widget);
	}

	public void update_widgets() {
		widgets.forEach(w -> w.update(this));
	}

	public void on_closed(final Player player) {}
	public final void closed(final Player player) {
		// TODO schedule_next_tick(() -> { on_closed(player); });
		on_closed(player);
	}

	public ClickResult on_click(final Player player, final ItemStack item, int slot, final ClickType type, final InventoryAction action) {
		return ClickResult.IGNORE;
	}

	public final void click(final Player player, final ItemStack item, int slot, final ClickType type, final InventoryAction action) {
		// Ignore unknown click actions
		if (action == InventoryAction.NOTHING || action == InventoryAction.UNKNOWN) {
			return;
		}

		// Send event to this menu
		var result = ClickResult.IGNORE;
		result = ClickResult.or(result, on_click(player, item, slot, type, action));

		// Send event to all widgets
		for (final var widget : widgets) {
			result = ClickResult.or(result, widget.click(player, this, item, slot, type, action));
		}

		switch (result) {
			default:
			case IGNORE: break;
			case SUCCESS: player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK,       SoundCategory.MASTER, 1.0f, 1.0f); break;
			case ERROR:   player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0f, 1.0f); break;
		}
	}

	public static boolean is_normal_click(ClickType type, InventoryAction action) {
		if (type != ClickType.LEFT) {
			return false;
		}

		switch (action) {
			case PICKUP_ALL:
			case PICKUP_HALF:
			case PICKUP_ONE:
			case PICKUP_SOME:
				return true;
		}

		return false;
	}

	public static enum ClickResult {
		SUCCESS(1),
		ERROR(2),
		IGNORE(0);

		private int priority;
		private ClickResult(int priority) {
			this.priority = priority;
		}

		public static ClickResult or(final ClickResult a, final ClickResult b) {
			return a.priority > b.priority ? a : b;
		}
	}
}
