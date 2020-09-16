package org.oddlama.vane.trifles.commands;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.trifles.Trifles;
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
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.module.Module;

@Name("heads")
public class Heads extends Command<Trifles> {
	public Heads(Context<Trifles> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::open_head_library);
	}

	private void open_head_library(final Player player) {
		MenuFactory.head_selector(get_context(), player, (player2, m, t) -> {
			m.close(player2);
			return ClickResult.SUCCESS;
		}, player2 -> { }).open(player);
	}
}
