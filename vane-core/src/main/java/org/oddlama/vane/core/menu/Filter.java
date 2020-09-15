package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.Nms.player_handle;

import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.ChatMessage;
import net.minecraft.server.v1_16_R2.ContainerAccess;
import net.minecraft.server.v1_16_R2.ContainerAnvil;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import net.minecraft.server.v1_16_R2.EntityHuman;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.PacketPlayOutOpenWindow;

import org.bukkit.entity.Player;

import java.util.List;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.module.Context;

public interface Filter<T> {
	public void open_filter_settings(final Context<?> context, final Player player, final Menu return_to);
	public List<T> filter(final List<T> things);

	public static class StringFilter<T> implements Filter<T> {
		private String str = null;
		private Function2<T, String, Boolean> do_filter;
		private boolean ignore_case;

		public StringFilter(final Function2<T, String, Boolean> do_filter) {
			this(do_filter, true);
		}

		public StringFilter(final Function2<T, String, Boolean> do_filter, boolean ignore_case) {
			this.do_filter = do_filter;
			this.ignore_case = ignore_case;
		}

		@Override
		public void open_filter_settings(final Context<?> context, final Player player, final Menu return_to) {
			MenuFactory.anvil_string_input(context, player, "Filter", new ItemStack(Material.PAPER), "Filter", (p, menu, s) -> {
				menu.close(p);
				str = s;
				return_to.open(p);
				return ClickResult.SUCCESS;
			}).open(player);
		}

		@Override
		public List<T> filter(final List<T> things) {
			if (str == null) {
				return things;
			} else {
				final String f_str;
				if (ignore_case) {
					f_str = str.toLowerCase();
				} else {
					f_str = str;
				}

				return things.stream()
					.filter(t -> do_filter.apply(t, f_str))
					.collect(Collectors.toList());
			}
		}
	}
}
