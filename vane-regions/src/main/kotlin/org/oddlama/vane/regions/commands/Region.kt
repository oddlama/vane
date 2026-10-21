package org.oddlama.vane.regions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions

@Name("region")
@Aliases("regions", "rg")
/** Root command opening the regions management menu. */
class Region(context: Context<Regions?>) :
    org.oddlama.vane.core.command.Command<Regions?>(context) {
    /** Builds the `/region` command tree. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .requires { source: CommandSourceStack -> source.sender is Player }
            .executes { ctx: CommandContext<CommandSourceStack> ->
                openMenu(ctx.source.sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    /** Opens the main regions menu for the given player. */
    private fun openMenu(player: Player) {
        module?.menus?.mainMenu?.create(player)?.open(player)
    }
}
