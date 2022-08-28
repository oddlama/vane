package org.oddlama.vane.core.menu;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.module.Context;

public class Menu {

	protected final MenuManager manager;
	protected Inventory inventory = null;
	private final Set<MenuWidget> widgets = new HashSet<>();
	private Consumer2<Player, InventoryCloseEvent.Reason> on_close = null;
	private Consumer1<Player> on_natural_close = null;
	private Object tag = null;

	// A tainted menu will refuse to be opened.
	// Useful to prevent an invalid menu from reopening
	// after it's state has been captured.
	protected boolean tainted = false;

	protected Menu(final Context<?> context) {
		this.manager = context.get_module().core.menu_manager;
	}

	public Menu(final Context<?> context, final Inventory inventory) {
		this.manager = context.get_module().core.menu_manager;
		this.inventory = inventory;
	}

	public MenuManager manager() {
		return manager;
	}

	public Inventory inventory() {
		return inventory;
	}

	public Object tag() {
		return tag;
	}

	public Menu tag(Object tag) {
		this.tag = tag;
		return this;
	}

	public void taint() {
		this.tainted = true;
	}

	public void add(final MenuWidget widget) {
		widgets.add(widget);
	}

	public boolean remove(final MenuWidget widget) {
		return widgets.remove(widget);
	}

	public void update() {
		update(false);
	}

	public void update(boolean force_update) {
		int updated = widgets.stream().mapToInt(w -> w.update(this) ? 1 : 0).sum();

		if (updated > 0 || force_update) {
			// Send inventory content to players
			manager.update(this);
		}
	}

	public void open_window(final Player player) {
		if (tainted) {
			return;
		}
		player.openInventory(inventory);
	}

	public final void open(final Player player) {
		if (tainted) {
			return;
		}
		update(true);
		manager.schedule_next_tick(() -> {
			manager.add(player, this);
			open_window(player);
		});
	}

	public boolean close(final Player player) {
		return close(player, InventoryCloseEvent.Reason.PLUGIN);
	}

	public boolean close(final Player player, final InventoryCloseEvent.Reason reason) {
		final var top_inventory = player.getOpenInventory().getTopInventory();
		if (top_inventory != inventory) {
			try {
				throw new RuntimeException("Invalid close from unrelated menu.");
			} catch (RuntimeException e) {
				manager
					.get_module()
					.log.log(
						Level.WARNING,
						"Tried to close menu inventory that isn't opened by the player " + player,
						e
					);
			}
			return false;
		}

		manager.schedule_next_tick(() -> player.closeInventory(reason));
		return true;
	}

	public Consumer2<Player, InventoryCloseEvent.Reason> get_on_close() {
		return on_close;
	}

	public Menu on_close(final Consumer2<Player, InventoryCloseEvent.Reason> on_close) {
		this.on_close = on_close;
		return this;
	}

	public Consumer1<Player> get_on_natural_close() {
		return on_natural_close;
	}

	public Menu on_natural_close(final Consumer1<Player> on_natural_close) {
		this.on_natural_close = on_natural_close;
		return this;
	}

	public final void closed(final Player player, final InventoryCloseEvent.Reason reason) {
		if (reason == InventoryCloseEvent.Reason.PLAYER && on_natural_close != null) {
			on_natural_close.apply(player);
		} else {
			if (on_close != null) {
				on_close.apply(player, reason);
			}
		}
		inventory.clear();
		manager.remove(player, this);
	}

	public ClickResult on_click(final Player player, final ItemStack item, int slot, final InventoryClickEvent event) {
		return ClickResult.IGNORE;
	}

	public final void click(final Player player, final ItemStack item, int slot, final InventoryClickEvent event) {
		// Ignore unknown click actions
		if (event.getAction() == InventoryAction.UNKNOWN) {
			return;
		}

		// Send event to this menu
		var result = ClickResult.IGNORE;
		result = ClickResult.or(result, on_click(player, item, slot, event));

		// Send event to all widgets
		for (final var widget : widgets) {
			result = ClickResult.or(result, widget.click(player, this, item, slot, event));
		}

		switch (result) {
			default:
			case INVALID_CLICK:
			case IGNORE:
				break;
			case SUCCESS:
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
				break;
			case ERROR:
				player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0f, 1.0f);
				break;
		}
	}

	public static boolean is_left_or_right_click(final InventoryClickEvent event) {
		final var type = event.getClick();
		return type == ClickType.LEFT || type == ClickType.RIGHT;
	}

	public static boolean is_left_click(final InventoryClickEvent event) {
		final var type = event.getClick();
		return type == ClickType.LEFT;
	}

	public static enum ClickResult {
		IGNORE(0),
		INVALID_CLICK(1),
		SUCCESS(2),
		ERROR(3);

		private int priority;

		private ClickResult(int priority) {
			this.priority = priority;
		}

		public static ClickResult or(final ClickResult a, final ClickResult b) {
			return a.priority > b.priority ? a : b;
		}
	}
}
