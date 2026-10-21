package org.oddlama.vane.trifles.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.StorageUtil

@Name("setspawn")
/** Command for setting the current world spawn and tagging the active spawn world. */
class Setspawn(context: Context<Trifles?>) :
    org.oddlama.vane.core.command.Command<Trifles?>(context) {
    /** Builds the `/setspawn` command tree. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { ctx: CommandSourceStack -> ctx.sender is Player }
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                setSpawn(ctx.getSource().sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    /** Sets spawn to the invoking player's location and updates spawn-world metadata. */
    private fun setSpawn(player: Player) {
        val loc = player.location
        val server = module?.server ?: return

        // Remove spawn-world marker from all worlds first.
        for (world in server.worlds) {
            world.persistentDataContainer.remove(IS_SPAWN_WORLD)
        }

        // Apply new spawn world marker and location.
        val world = player.world
        world.spawnLocation = loc
        world.persistentDataContainer.set(IS_SPAWN_WORLD, PersistentDataType.INTEGER, 1)
        player.sendMessage("§aSpawn §7set!")
    }

    companion object {
        @JvmField
        /** Metadata key identifying which world acts as global spawn target. */
        val IS_SPAWN_WORLD: NamespacedKey = StorageUtil.namespacedKey("vane", "is_spawn_world")
    }
}
