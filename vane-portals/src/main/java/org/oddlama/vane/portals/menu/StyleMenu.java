package org.oddlama.vane.portals.menu;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.Style;

public class StyleMenu extends ModuleComponent<Portals> {

	private static final int columns = 9;

	@LangMessage
	public TranslatedMessage lang_title;

	@LangMessage
	public TranslatedMessage lang_select_block_console_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_origin_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_portal_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_1_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_2_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_3_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_4_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_5_active_title;

	@LangMessage
	public TranslatedMessage lang_select_block_console_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_origin_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_portal_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_1_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_2_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_3_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_4_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_block_boundardy_5_inactive_title;

	@LangMessage
	public TranslatedMessage lang_select_style_title;

	@LangMessage
	public TranslatedMessage lang_filter_styles_title;

	private TranslatedItemStack<?> item_block_console_active;
	private TranslatedItemStack<?> item_block_origin_active;
	private TranslatedItemStack<?> item_block_portal_active;
	private TranslatedItemStack<?> item_block_boundardy_1_active;
	private TranslatedItemStack<?> item_block_boundardy_2_active;
	private TranslatedItemStack<?> item_block_boundardy_3_active;
	private TranslatedItemStack<?> item_block_boundardy_4_active;
	private TranslatedItemStack<?> item_block_boundardy_5_active;
	private TranslatedItemStack<?> item_block_console_inactive;
	private TranslatedItemStack<?> item_block_origin_inactive;
	private TranslatedItemStack<?> item_block_portal_inactive;
	private TranslatedItemStack<?> item_block_boundardy_1_inactive;
	private TranslatedItemStack<?> item_block_boundardy_2_inactive;
	private TranslatedItemStack<?> item_block_boundardy_3_inactive;
	private TranslatedItemStack<?> item_block_boundardy_4_inactive;
	private TranslatedItemStack<?> item_block_boundardy_5_inactive;

	private TranslatedItemStack<?> item_accept;
	private TranslatedItemStack<?> item_reset;
	private TranslatedItemStack<?> item_select_defined;
	private TranslatedItemStack<?> item_select_style;
	private TranslatedItemStack<?> item_cancel;

	public StyleMenu(Context<Portals> context) {
		super(context.namespace("style"));
		final var ctx = get_context();
		item_block_console_active =
			new TranslatedItemStack<>(
				ctx,
				"block_console_active",
				Material.BARRIER,
				1,
				"Used to select active console block."
			);
		item_block_origin_active =
			new TranslatedItemStack<>(
				ctx,
				"block_origin_active",
				Material.BARRIER,
				1,
				"Used to select active origin block."
			);
		item_block_portal_active =
			new TranslatedItemStack<>(
				ctx,
				"block_portal_active",
				Material.BARRIER,
				1,
				"Used to select active portal area block. Defaults to end gateway if unset."
			);
		item_block_boundardy_1_active =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_1_active",
				Material.BARRIER,
				1,
				"Used to select active boundary variant 1 block."
			);
		item_block_boundardy_2_active =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_2_active",
				Material.BARRIER,
				1,
				"Used to select active boundary variant 2 block."
			);
		item_block_boundardy_3_active =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_3_active",
				Material.BARRIER,
				1,
				"Used to select active boundary variant 3 block."
			);
		item_block_boundardy_4_active =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_4_active",
				Material.BARRIER,
				1,
				"Used to select active boundary variant 4 block."
			);
		item_block_boundardy_5_active =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_5_active",
				Material.BARRIER,
				1,
				"Used to select active boundary variant 5 block."
			);
		item_block_console_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_console_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive console block."
			);
		item_block_origin_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_origin_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive origin block."
			);
		item_block_portal_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_portal_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive portal area block."
			);
		item_block_boundardy_1_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_1_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive boundary variant 1 block."
			);
		item_block_boundardy_2_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_2_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive boundary variant 2 block."
			);
		item_block_boundardy_3_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_3_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive boundary variant 3 block."
			);
		item_block_boundardy_4_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_4_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive boundary variant 4 block."
			);
		item_block_boundardy_5_inactive =
			new TranslatedItemStack<>(
				ctx,
				"block_boundardy_5_inactive",
				Material.BARRIER,
				1,
				"Used to select inactive boundary variant 5 block."
			);

		item_accept = new TranslatedItemStack<>(ctx, "accept", Material.LIME_TERRACOTTA, 1, "Used to apply the style.");
		item_reset = new TranslatedItemStack<>(ctx, "reset", Material.MILK_BUCKET, 1, "Used to reset any changes.");
		item_select_defined =
			new TranslatedItemStack<>(
				ctx,
				"select_defined",
				Material.ITEM_FRAME,
				1,
				"Used to select a defined style from the configuration."
			);
		item_select_style =
			new TranslatedItemStack<>(
				ctx,
				"select_style",
				Material.ITEM_FRAME,
				1,
				"Used to represent a defined style in the selector menu."
			);
		item_cancel =
			new TranslatedItemStack<>(ctx, "cancel", Material.RED_TERRACOTTA, 1, "Used to abort style selection.");
	}

	public Menu create(final Portal portal, final Player player, final Menu previous) {
		final var title = lang_title.str_component("§5§l" + portal.name());
		final var style_menu = new Menu(get_context(), Bukkit.createInventory(null, 4 * columns, title));
		style_menu.tag(new PortalMenuTag(portal.id()));

		final var style_container = new StyleContainer();
		style_container.defined_style = portal.style();
		style_container.style = portal.copy_style(get_module(), null);

		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
					0,
				item_block_console_inactive,
				get_module().constructor.config_material_console,
				lang_select_block_console_inactive_title.str(),
				PortalBlock.Type.CONSOLE,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
					1,
				item_block_origin_inactive,
				get_module().constructor.config_material_origin,
				lang_select_block_origin_inactive_title.str(),
				PortalBlock.Type.ORIGIN,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
					2,
				item_block_portal_inactive,
				get_module().constructor.config_material_portal_area,
				lang_select_block_portal_inactive_title.str(),
				PortalBlock.Type.PORTAL,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				4,
				item_block_boundardy_1_inactive,
				get_module().constructor.config_material_boundary_1,
				lang_select_block_boundardy_1_inactive_title.str(),
				PortalBlock.Type.BOUNDARY_1,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				5,
				item_block_boundardy_2_inactive,
				get_module().constructor.config_material_boundary_2,
				lang_select_block_boundardy_2_inactive_title.str(),
				PortalBlock.Type.BOUNDARY_2,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				6,
				item_block_boundardy_3_inactive,
				get_module().constructor.config_material_boundary_3,
				lang_select_block_boundardy_3_inactive_title.str(),
				PortalBlock.Type.BOUNDARY_3,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				7,
				item_block_boundardy_4_inactive,
				get_module().constructor.config_material_boundary_4,
				lang_select_block_boundardy_4_inactive_title.str(),
				PortalBlock.Type.BOUNDARY_4,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				8,
				item_block_boundardy_5_inactive,
				get_module().constructor.config_material_boundary_5,
				lang_select_block_boundardy_5_inactive_title.str(),
				PortalBlock.Type.BOUNDARY_5,
				false
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
					columns,
				item_block_console_active,
				get_module().constructor.config_material_console,
				lang_select_block_console_active_title.str(),
				PortalBlock.Type.CONSOLE,
				true
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 1,
				item_block_origin_active,
				get_module().constructor.config_material_origin,
				lang_select_block_origin_active_title.str(),
				PortalBlock.Type.ORIGIN,
				true
			)
		);
		//  style_menu.add(menu_item_block_selector(portal, style_container, 1 * columns + 2, item_block_portal_active,        get_module().constructor.config_material_portal_area, lang_select_block_portal_active_title.str(),        PortalBlock.Type.PORTAL,     true));
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 4,
				item_block_boundardy_1_active,
				get_module().constructor.config_material_boundary_1,
				lang_select_block_boundardy_1_active_title.str(),
				PortalBlock.Type.BOUNDARY_1,
				true
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 5,
				item_block_boundardy_2_active,
				get_module().constructor.config_material_boundary_2,
				lang_select_block_boundardy_2_active_title.str(),
				PortalBlock.Type.BOUNDARY_2,
				true
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 6,
				item_block_boundardy_3_active,
				get_module().constructor.config_material_boundary_3,
				lang_select_block_boundardy_3_active_title.str(),
				PortalBlock.Type.BOUNDARY_3,
				true
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 7,
				item_block_boundardy_4_active,
				get_module().constructor.config_material_boundary_4,
				lang_select_block_boundardy_4_active_title.str(),
				PortalBlock.Type.BOUNDARY_4,
				true
			)
		);
		style_menu.add(
			menu_item_block_selector(
				portal,
				style_container,
				columns + 8,
				item_block_boundardy_5_active,
				get_module().constructor.config_material_boundary_5,
				lang_select_block_boundardy_5_active_title.str(),
				PortalBlock.Type.BOUNDARY_5,
				true
			)
		);

		style_menu.add(menu_item_accept(portal, style_container, previous));
		style_menu.add(menu_item_reset(portal, style_container));
		style_menu.add(menu_item_select_defined(portal, style_container));
		style_menu.add(menu_item_cancel(previous));

		style_menu.on_natural_close(player2 -> previous.open(player2));
		return style_menu;
	}

	private static ItemStack item_for_type(
		final StyleContainer style_container,
		final boolean active,
		final PortalBlock.Type type
	) {
		if (active && type == PortalBlock.Type.PORTAL) {
			return new ItemStack(Material.AIR);
		}
		return new ItemStack(style_container.style.material(active, type));
	}

	private MenuWidget menu_item_block_selector(
		final Portal portal,
		final StyleContainer style_container,
		int slot,
		final TranslatedItemStack<?> t_item,
		final Material building_material,
		final String title,
		final PortalBlock.Type type,
		final boolean active
	) {
		return new MenuItem(
			slot,
			null,
			(player, menu, self) -> {
				menu.close(player);
				MenuFactory
					.item_selector(
						get_context(),
						player,
						title,
						item_for_type(style_container, active, type),
						true,
						(player2, item) -> {
							style_container.defined_style = null;
							if (item == null) {
								if (active && type == PortalBlock.Type.PORTAL) {
									style_container.style.set_material(active, type, Material.END_GATEWAY, true);
								}
								style_container.style.set_material(active, type, Material.AIR, true);
							} else {
								style_container.style.set_material(active, type, item.getType(), true);
							}
							menu.open(player2);
							return ClickResult.SUCCESS;
						},
						player2 -> menu.open(player2),
						item -> {
							// Only allow placeable solid blocks
							if (item == null || !(item.getType().isBlock() && item.getType().isSolid())) {
								return null;
							}
							// Always select one
							item.setAmount(1);
							return item;
						}
					)
					.tag(new PortalMenuTag(portal.id()))
					.open(player);
				menu.update();
				return ClickResult.SUCCESS;
			}
		) {
			@Override
			public void item(final ItemStack item) {
				final var stack = item_for_type(style_container, active, type);
				if (stack.getType() == Material.AIR) {
					stack.setType(Material.BARRIER);
				}
				super.item(t_item.alternative(stack, "§6" + building_material.getKey()));
			}
		};
	}

	private MenuWidget menu_item_accept(
		final Portal portal,
		final StyleContainer style_container,
		final Menu previous
	) {
		return new MenuItem(
				3 * columns,
			item_accept.item(),
			(player, menu, self) -> {
				menu.close(player);

				final var settings_event = new PortalChangeSettingsEvent(player, portal, false);
				get_module().getServer().getPluginManager().callEvent(settings_event);
				if (settings_event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
					return ClickResult.ERROR;
				}

				portal.style(style_container.style);
				portal.update_blocks(get_module());
				previous.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_reset(final Portal portal, final StyleContainer style_container) {
		return new MenuItem(
			3 * columns + 3,
			item_reset.item(),
			(player, menu, self) -> {
				style_container.style = portal.copy_style(get_module(), null);
				menu.update();
				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_select_defined(final Portal portal, final StyleContainer style_container) {
		final Function1<Style, ItemStack> item_for = style -> {
			final var mat = style.material(false, PortalBlock.Type.BOUNDARY_1);
			if (mat == null) {
				return new ItemStack(Material.BARRIER);
			} else {
				return new ItemStack(mat);
			}
		};

		return new MenuItem(
			3 * columns + 4,
			item_select_defined.item(),
			(player, menu, self) -> {
				menu.close(player);
				final var all_styles = new ArrayList<>(get_module().styles.values());
				final var filter = new Filter.StringFilter<Style>((s, str) ->
					s.key().toString().toLowerCase().contains(str)
				);
				MenuFactory
					.generic_selector(
						get_context(),
						player,
						lang_select_style_title.str(),
						lang_filter_styles_title.str(),
						all_styles,
						s -> item_select_style.alternative(item_for.apply(s), s.key().getKey()),
						filter,
						(player2, m, t) -> {
							m.close(player2);
							style_container.defined_style = t.key();
							style_container.style = t.copy(null);
							menu.open(player2);
							return ClickResult.SUCCESS;
						},
						player2 -> menu.open(player2)
					)
					.tag(new PortalMenuTag(portal.id()))
					.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	private MenuWidget menu_item_cancel(final Menu previous) {
		return new MenuItem(
			3 * columns + 8,
			item_cancel.item(),
			(player, menu, self) -> {
				menu.close(player);
				previous.open(player);
				return ClickResult.SUCCESS;
			}
		);
	}

	@Override
	public void on_enable() {}

	@Override
	public void on_disable() {}

	private static class StyleContainer {

		public NamespacedKey defined_style = null;
		public Style style = null;
	}
}
