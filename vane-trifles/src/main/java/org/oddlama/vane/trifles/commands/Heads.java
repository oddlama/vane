package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static org.oddlama.vane.util.PlayerUtil.give_items;
import static org.oddlama.vane.util.PlayerUtil.take_items;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@Name("heads")
public class Heads extends Command<Trifles> {

    @ConfigMaterial(def = Material.BONE, desc = "Currency material used to buy heads.")
    public Material config_currency;

    @ConfigInt(def = 1, min = 0, desc = "Price (in currency) per head. Set to 0 for free heads.")
    public int config_price_per_head;

    public Heads(Context<Trifles> context) {
        // Anyone may use this by default.
        super(context, PermissionDefault.TRUE);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
            .requires(ctx -> ctx.getSender() instanceof Player)
            .then(help())
            .executes(ctx -> {
                open_head_library((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void open_head_library(final Player player) {
        MenuFactory.head_selector(
            get_context(),
            player,
            (player2, m, t, event) -> {
                final int amount;
                switch (event.getClick()) {
                    default:
                        return ClickResult.INVALID_CLICK;
                    case NUMBER_KEY:
                        amount = event.getHotbarButton() + 1;
                        break;
                    case LEFT:
                        amount = 1;
                        break;
                    case RIGHT:
                        amount = 32;
                        break;
                    case MIDDLE:
                    case SHIFT_LEFT:
                        amount = 64;
                        break;
                    case SHIFT_RIGHT:
                        amount = 16;
                        break;
                }

                // Take currency items
                if (
                    config_price_per_head > 0 &&
                    !take_items(player2, new ItemStack(config_currency, config_price_per_head * amount))
                ) {
                    return ClickResult.ERROR;
                }

                give_items(player2, t.item(), amount);
                return ClickResult.SUCCESS;
            },
            player2 -> {}
        ).open(player);
    }
}
