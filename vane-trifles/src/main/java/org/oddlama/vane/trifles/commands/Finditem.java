package org.oddlama.vane.trifles.commands;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@Name("finditem")
public class Finditem extends Command<Trifles> {
	public Finditem(Context<Trifles> context) {
		// Anyone may use this by default.
		super(context, PermissionDefault.TRUE);
		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().choice("material", List.of(Material.values()), m -> m.getKey().toString())
			.ignore_case()
			.exec_player(get_module().item_finder::find_item);
	}
}
