package org.oddlama.vane.core.commands;

import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.PlayerUtil;

@Name("customitem")
public class CustomItem extends Command<Core> {

	public CustomItem(Context<Core> context) {
		super(context);
		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);

		// Give custom item
		params()
			.fixed("give")
			.choice(
				"custom_item",
				sender -> get_module().item_registry().all(),
				(sender, e) -> e.key().toString(),
				(sender, e) ->
					get_module().item_registry().all()
						.stream()
						.filter(i -> i.key().toString().equalsIgnoreCase(e))
						.findFirst()
						.orElse(null)
			)
			.exec_player(this::give_custom_item);
	}

	private void give_custom_item(final Player player, final org.oddlama.vane.core.item.api.CustomItem custom_item) {
		PlayerUtil.give_item(player, custom_item.newStack());
	}
}
