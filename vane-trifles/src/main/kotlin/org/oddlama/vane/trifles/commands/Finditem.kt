package org.oddlama.vane.trifles.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@Name("finditem")
/** Command entry point for searching nearby inventories by material. */
class Finditem(context: Context<Trifles?>) :
    org.oddlama.vane.core.command.Command<Trifles?>(context, PermissionDefault.TRUE) {
    /** Builds the `/finditem` command tree. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .then(
                Commands.argument("material", ArgumentTypes.resource(RegistryKey.ITEM)).executes {
                    ctx: CommandContext<CommandSourceStack> ->
                    val itemType = ctx.getArgument("material", ItemType::class.java)
                    val material = Material.matchMaterial(itemType.key.key)

                    val module = module
                    if (material != null && module != null) {
                        module.itemFinder?.findItem(ctx.source.sender as Player, material)
                    }
                    Command.SINGLE_SUCCESS
                }
            )
    }
}
