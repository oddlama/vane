package org.oddlama.vane.regions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;

@Name("region")
@Aliases({ "regions", "rg" })
public class Region extends Command<Regions> {

    public Region(Context<Regions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
            .then(help())
            .then(
                LiteralArgumentBuilder.<CommandSourceStack>literal("visualize")
                    .requires(ctx -> ctx.getSender() instanceof Player)
                    .executes(ctx -> {
                        toggle_region_visualization((Player) ctx.getSource().getSender());
                        return SINGLE_SUCCESS;
                    })
            )
            .requires(ctx -> ctx.getSender() instanceof Player)
            .executes(ctx -> {
                open_menu((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void open_menu(Player player) {
        get_module().menus.main_menu.create(player).open(player);
    }
    
    private void toggle_region_visualization(Player player) {
        final var player_id = player.getUniqueId();
        
        if (get_module().is_visualizing_region(player_id)) {
            get_module().stop_visualizing_region(player_id);
            player.sendMessage("§cRegion visualization disabled.");
        } else {
            final var region = get_module().region_at(player.getLocation());
            if (region == null) {
                player.sendMessage("§cYou must be inside a region to visualize it.");
                return;
            }
            
            get_module().start_visualizing_region(player_id, region);
            player.sendMessage("§aVisualizing region boundaries. Use §b/region visualize§a again to disable.");
        }
    }
}
