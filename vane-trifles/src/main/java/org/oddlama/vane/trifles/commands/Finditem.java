package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@Name("finditem")
public class Finditem extends Command<Trifles> {

    public Finditem(Context<Trifles> context) {
        super(context, PermissionDefault.TRUE);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
            .then(help())
            .then(
                argument("material", ArgumentTypes.resource(RegistryKey.ITEM)).executes(ctx -> {
                    get_module()
                        .item_finder.find_item(
                            (Player) ctx.getSource().getSender(),
                            ctx.getArgument("material", ItemType.class).asMaterial()
                        );
                    return SINGLE_SUCCESS;
                })
            );
    }
}
