package org.oddlama.vane.core.menu;

import static org.oddlama.vane.util.Nms.player_handle;

import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.ChatMessage;
import net.minecraft.server.v1_16_R2.ContainerAccess;
import net.minecraft.server.v1_16_R2.ContainerAnvil;
import java.util.Objects;
import java.util.ArrayList;
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
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.menu.MenuItem;
import org.oddlama.vane.core.menu.MenuWidget;
import org.oddlama.vane.core.material.HeadMaterial;
import org.oddlama.vane.core.material.HeadMaterialLibrary;
import org.oddlama.vane.core.menu.Filter;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import net.minecraft.server.v1_16_R2.EntityHuman;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.PacketPlayOutOpenWindow;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.module.Context;

public class HeadFilter implements Filter<HeadMaterial> {
	private String str = null;

	public HeadFilter() {}

	@Override
	public void open_filter_settings(final Context<?> context, final Player player, final String filter_title, final Menu return_to) {
		MenuFactory.anvil_string_input(context, player, filter_title, new ItemStack(Material.PAPER), "?", (p, menu, s) -> {
			menu.close(p);
			str = s.toLowerCase();
			return_to.open(p);
			return ClickResult.SUCCESS;
		}).open(player);
	}

	@Override
	public void reset() {
		str = null;
	}

	private boolean filter_by_categories(final HeadMaterial material) {
		return material.category().toLowerCase().contains(str);
	}

	private boolean filter_by_tags(final HeadMaterial material) {
		for (final var tag : material.tags()) {
			if (tag.toLowerCase().contains(str)) {
				return true;
			}
		}

		return false;
	}

	private boolean filter_by_name(final HeadMaterial material) {
		return material.name().toLowerCase().contains(str);
	}

	@Override
	public List<HeadMaterial> filter(final List<HeadMaterial> things) {
		if (str == null) {
			return things;
		}

		return things.stream()
			.filter(t -> filter_by_categories(t) || filter_by_tags(t) || filter_by_name(t))
			.collect(Collectors.toList());
	}
}
