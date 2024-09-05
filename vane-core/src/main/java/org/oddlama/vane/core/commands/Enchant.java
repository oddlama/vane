package org.oddlama.vane.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.EnchantmentFilterArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;

@Name("enchant")
public class Enchant extends Command<Core> {

	@LangMessage
	private TranslatedMessage lang_level_too_low;

	@LangMessage
	private TranslatedMessage lang_level_too_high;

	@LangMessage
	private TranslatedMessage lang_invalid_enchantment;

	public Enchant(Context<Core> context) {
		super(context);
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {

		return super.get_command_base()
			.requires(ctx -> ctx.getSender() instanceof Player)
			.then(help())
			.then(
				argument("enchantment", EnchantmentFilterArgumentType.enchantmentFilter())
				.executes(ctx -> {enchant_current_item_level_1((Player) ctx.getSource().getSender(), enchantment(ctx)); return SINGLE_SUCCESS;})
				.then(argument("level", IntegerArgumentType.integer(1))
					.executes(ctx -> {enchant_current_item((Player) ctx.getSource().getSender(), enchantment(ctx), ctx.getArgument("level", Integer.class)); return SINGLE_SUCCESS;})
				)
			);
	}

	private Enchantment enchantment(CommandContext<CommandSourceStack> ctx){
		return ctx.getArgument("enchantment", Enchantment.class);
	}

	private boolean filter_by_held_item(CommandSender sender, Enchantment e) {
		if (!(sender instanceof Player)) {
			return false;
		}

		final var player = (Player) sender;
		final var item_stack = player.getEquipment().getItemInMainHand();
		boolean is_book = item_stack.getType() == Material.BOOK || item_stack.getType() == Material.ENCHANTED_BOOK;
		return is_book || e.canEnchantItem(item_stack);
	}

	private void enchant_current_item_level_1(Player player, Enchantment enchantment) {
		enchant_current_item(player, enchantment, 1);
	}

	private void enchant_current_item(Player player, Enchantment enchantment, Integer level) {
		if (level < enchantment.getStartLevel()) {
			lang_level_too_low.send(player, "§b" + level, "§a" + enchantment.getStartLevel());
			return;
		} else if (level > enchantment.getMaxLevel()) {
			lang_level_too_high.send(player, "§b" + level, "§a" + enchantment.getMaxLevel());
			return;
		}

		final var item_stack = player.getEquipment().getItemInMainHand();
		if (item_stack.getType() == Material.AIR) {
			lang_invalid_enchantment.send(
				player,
				"§b" + enchantment.getKey(),
				"§a" + item_stack.getType().getKey()
			);
			return;
		}

		try {
			// Convert a book if necessary
			if (item_stack.getType() == Material.BOOK) {
				// FIXME this technically yields wrong items when this was a tome,
				// as just changing the base item is not equivalent to custom item conversion.
				// The custom model data and item tag will still be those of a book.
				// The fix is not straightforward without hardcoding tome identifiers,
				// so for now we leave it as is.
				item_stack.setType(Material.ENCHANTED_BOOK);
				/* fallthrough */
			}

			if (item_stack.getType() == Material.ENCHANTED_BOOK) {
				final var meta = (EnchantmentStorageMeta) item_stack.getItemMeta();
				meta.addStoredEnchant(enchantment, level, false);
				item_stack.setItemMeta(meta);
			} else {
				item_stack.addEnchantment(enchantment, level);
			}

			get_module().enchantment_manager.update_enchanted_item(item_stack);
		} catch (Exception e) {
			lang_invalid_enchantment.send(
				player,
				"§b" + enchantment.getKey(),
				"§a" + item_stack.getType().getKey()
			);
		}
	}
}
