package org.oddlama.vane.core.commands;

import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.item.CustomItemVariant;
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
				sender -> all_custom_items(),
				(sender, e) -> e.key().toString(),
				(sender, e) ->
					all_custom_items()
						.stream()
						.filter(i -> i.key().toString().equalsIgnoreCase(e))
						.findFirst()
						.orElse(null)
			)
			.exec_player(this::give_custom_item);
	}

	private Collection<CustomItemVariant<?, ?, ?>> all_custom_items() {
		final ArrayList<CustomItemVariant<?, ?, ?>> list = new ArrayList<>();
		get_module()
			.core.for_all_module_components(c -> {
				if (c instanceof CustomItemVariant) {
					list.add((CustomItemVariant<?, ?, ?>) c);
				}
			});
		return list;
	}

	private void give_custom_item(final Player player, final CustomItemVariant<?, ?, ?> custom_item_variant) {
		PlayerUtil.give_item(player, custom_item_variant.item());
	}
}
