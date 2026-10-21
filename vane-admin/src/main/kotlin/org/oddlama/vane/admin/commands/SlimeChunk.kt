package org.oddlama.vane.admin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/** Command that reports whether the player's current chunk is a slime chunk. */
@Name("slimechunk")
class SlimeChunk(context: Context<Admin?>) :
    org.oddlama.vane.core.command.Command<Admin?>(context) {
    @LangMessage private val langSlimeChunkYes: TranslatedMessage? = null

    @LangMessage private val langSlimeChunkNo: TranslatedMessage? = null

    /** Builds the command tree for querying slime chunk state. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { it.sender is Player }
            .then(help())
            .executes { ctx ->
                isSlimeChunk(ctx.source.sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    /** Sends the slime chunk status for the given player's current chunk. */
    private fun isSlimeChunk(player: Player) {
        (if (player.location.chunk.isSlimeChunk) langSlimeChunkYes else langSlimeChunkNo)!!.send(
            player
        )
    }
}
