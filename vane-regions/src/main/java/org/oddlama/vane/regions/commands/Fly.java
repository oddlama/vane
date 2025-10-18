package org.oddlama.vane.regions.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.RoleSetting;

@Name("fly")
@Aliases({ "regionfly" })
public class Fly extends Command<Regions> {

    @LangMessage
    public TranslatedMessage lang_fly_enabled;

    @LangMessage
    public TranslatedMessage lang_fly_disabled;

    @LangMessage
    public TranslatedMessage lang_fly_not_in_region;

    @LangMessage
    public TranslatedMessage lang_fly_no_permission;

    public Fly(Context<Regions> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
            .then(help())
            .requires(ctx -> ctx.getSender() instanceof Player)
            .executes(ctx -> {
                toggle_fly((Player) ctx.getSource().getSender());
                return SINGLE_SUCCESS;
            });
    }

    private void toggle_fly(Player player) {
        // Check if player is in a region
        final var region = get_module().region_at(player.getLocation());
        
        if (region == null) {
            lang_fly_not_in_region.send(player);
            return;
        }

        // Get the region group and check permissions
        final var group = region.region_group(get_module());
        final var role = group.get_role(player.getUniqueId());
        
        // Check if player has BUILD permission (which friends and admins have)
        // or ADMIN permission
        final boolean hasPermission = role.get_setting(RoleSetting.ADMIN) || 
                                     role.get_setting(RoleSetting.BUILD);
        
        if (!hasPermission) {
            lang_fly_no_permission.send(player);
            return;
        }

        // Toggle flying
        if (player.getAllowFlight()) {
            // Disable flying
            player.setAllowFlight(false);
            player.setFlying(false);
            get_module().fly_manager.remove_flying_player(player.getUniqueId());
            // Set manual opt-out so auto-fly won't re-enable until toggled again
            get_module().fly_manager.set_manual_opt_out(player.getUniqueId(), true);
            // Stop visualizing the region
            get_module().stop_visualizing_region(player.getUniqueId());
            lang_fly_disabled.send(player);
        } else {
            // Enable flying
            player.setAllowFlight(true);
            player.setFlying(true);
            get_module().fly_manager.add_flying_player(player.getUniqueId(), region);
            // Clear manual opt-out now that the player opted in again
            get_module().fly_manager.set_manual_opt_out(player.getUniqueId(), false);
            // Start visualizing the region
            get_module().start_visualizing_region(player.getUniqueId(), region);
            lang_fly_enabled.send(player);
        }
    }
}

