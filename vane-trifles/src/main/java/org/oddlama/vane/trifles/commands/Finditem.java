package org.oddlama.vane.trifles.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import java.util.Locale;
import org.bukkit.Material;
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
                    final var sender = ctx.getSource().getSender();
                    final var player = (Player) sender;

                    final var item_type = ctx.getArgument("material", ItemType.class);

                    // Attempt to resolve to a Material without using the deprecated asMaterial().
                    Material material = null;
                    final var repr = item_type.toString(); // typically namespace:key

                    // Try direct match with the full representation first.
                    material = Material.matchMaterial(repr);

                    // If that failed, try the key part after ':' and map to enum name.
                    if (material == null) {
                        final var name_part = repr.contains(":") ? repr.substring(repr.indexOf(':') + 1) : repr;
                        material = Material.matchMaterial(name_part);
                        if (material == null) {
                            material = Material.getMaterial(name_part.toUpperCase(Locale.ROOT));
                        }
                    }

                    if (material == null) {
                        player.sendMessage("Unknown material: " + repr);
                        return SINGLE_SUCCESS;
                    }

                    get_module().item_finder.find_item(player, material);
                    return SINGLE_SUCCESS;
                })
            );
    }
}
