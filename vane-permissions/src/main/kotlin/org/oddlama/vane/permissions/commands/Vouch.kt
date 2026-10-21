package org.oddlama.vane.permissions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import java.util.*
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.permissions.Permissions

@Name("vouch")
/**
 * Lets players vouch for other players and assigns a configured group to the target when they are
 * vouched for the first time.
 *
 * @param context command context bound to the permissions module.
 */
class Vouch(context: Context<Permissions?>) :
    org.oddlama.vane.core.command.Command<Permissions?>(context) {
    /** Non-null typed accessor for the backing permissions module. */
    private val permissionsModule: Permissions
        get() = requireNotNull(module)

    /** Confirmation message shown when a player successfully vouches for another player. */
    @LangMessage private val langVouched: TranslatedMessage? = null

    /** Message shown when the sender has already vouched for the target. */
    @LangMessage private val langAlreadyVouched: TranslatedMessage? = null

    /** Group assigned when a player receives their first vouch. */
    @ConfigString(
        def = "user",
        desc = "The group to assign to players when someone vouches for them.",
        metrics = true,
    )
    private val configVouchGroup: String? = null

    /**
     * Persistent mapping from a target player's UUID to the set of UUIDs of players who vouched for
     * them.
     */
    @Persistent var storageVouchedBy: MutableMap<UUID, MutableSet<UUID>> = HashMap()

    /** Builds the command tree for `/vouch <offline_player>`. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { it.sender is Player }
            .then(help())
            .then(
                Commands.argument<OfflinePlayer>(
                        "offline_player",
                        OfflinePlayerArgumentType.offlinePlayer(),
                    )
                    .executes { ctx ->
                        val sender = ctx.source.sender as? Player ?: return@executes 0
                        vouchForPlayer(
                            sender,
                            ctx.getArgument("offline_player", OfflinePlayer::class.java),
                        )
                        Command.SINGLE_SUCCESS
                    }
            )
    }

    /** Ensures persisted storage is mutable before write operations. */
    private fun ensureMutableStorage() {
        storageVouchedBy =
            storageVouchedBy.mapValues { (_, vouchedBy) -> vouchedBy.toMutableSet() }.toMutableMap()
    }

    /**
     * Records that a player vouched for another player and assigns the configured group to the
     * target when they receive their first vouch.
     */
    private fun vouchForPlayer(sender: Player, vouchedPlayer: OfflinePlayer) {
        ensureMutableStorage()
        val vouchedBySet = storageVouchedBy.getOrPut(vouchedPlayer.uniqueId) { mutableSetOf() }

        if (vouchedBySet.add(sender.uniqueId)) {
            if (vouchedBySet.size == 1) {
                permissionsModule.addPlayerToGroup(vouchedPlayer, configVouchGroup)
            }

            langVouched!!.send(sender, "§b${vouchedPlayer.name}")
        } else {
            langAlreadyVouched!!.send(sender, "§b${vouchedPlayer.name}")
        }

        markPersistentStorageDirty()
    }
}
