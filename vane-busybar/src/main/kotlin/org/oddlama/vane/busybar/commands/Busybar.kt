package org.oddlama.vane.busybar.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.busybar.BusyBar
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/**
 * Lets players link, unlink and inspect their BUSY Bar bridge and switch focus mode in the game,
 * and lets admins list linked players and clear or revoke someone else's.
 *
 * @param context owning context.
 */
@Name("busybar")
class Busybar(context: Context<BusyBar?>) :
    org.oddlama.vane.core.command.Command<BusyBar?>(context, PermissionDefault.TRUE) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Required for `list`, `clear`, `revoke` and `rotatekey`. */
    private val adminPermission =
        Permission(
                "vane.${busybar.annotationName}.admin",
                "Manage other players' BUSY Bars with /busybar list, clear and revoke, and replace the TLS key with rotatekey",
                PermissionDefault.OP,
            )
            .also(busybar::registerPermission)

    /** Sent after a token was issued. The pairing string follows in a separate line. */
    @LangMessage var langLinked: TranslatedMessage? = null

    /** Hover text of the pairing string. */
    @LangMessage var langClickToCopy: TranslatedMessage? = null

    /**
     * `/busybar link` while the server's address is unknown, so no usable pairing string exists.
     */
    @LangMessage var langNoPublicUrl: TranslatedMessage? = null

    /** Sent after the token was revoked. */
    @LangMessage var langUnlinked: TranslatedMessage? = null

    /** Sent when the player has no token. */
    @LangMessage var langNotLinked: TranslatedMessage? = null

    /** Status of a linked player. Takes the number of connected bridges and the focus state. */
    @LangMessage var langStatus: TranslatedMessage? = null

    /** Focus state shown in messages while busy. */
    @LangMessage var langBusyOn: TranslatedMessage? = null

    /** Focus state shown in messages while not busy. */
    @LangMessage var langBusyOff: TranslatedMessage? = null

    /** Confirms `/busybar busy on|off`. Takes the new focus state. */
    @LangMessage var langBusySet: TranslatedMessage? = null

    /** Header of `/busybar list`. Takes the number of players. */
    @LangMessage var langListHeader: TranslatedMessage? = null

    /**
     * One player in `/busybar list`. Takes the name, connected bridges, focus state, and whether a
     * token exists.
     */
    @LangMessage var langListEntry: TranslatedMessage? = null

    /** `/busybar list` when nobody is linked or busy. */
    @LangMessage var langListEmpty: TranslatedMessage? = null

    /** Shown in `/busybar list` for players with a token. */
    @LangMessage var langLinkedYes: TranslatedMessage? = null

    /** Shown in `/busybar list` for busy players without a token. */
    @LangMessage var langLinkedNo: TranslatedMessage? = null

    /** Confirms `/busybar clear`. Takes the player name. */
    @LangMessage var langCleared: TranslatedMessage? = null

    /** Confirms `/busybar revoke`. Takes the player name. */
    @LangMessage var langRevoked: TranslatedMessage? = null

    /** The target of `clear` or `revoke` has nothing to clear or revoke. Takes the player name. */
    @LangMessage var langNothingToDo: TranslatedMessage? = null

    /** Confirms `/busybar rotatekey`. */
    @LangMessage var langKeyRotated: TranslatedMessage? = null

    /** `/busybar rotatekey` while TLS is not in auto mode. */
    @LangMessage var langKeyNotRotatable: TranslatedMessage? = null

    /** Any `/busybar` subcommand while a proxy manages BUSY Bars. */
    @LangMessage var langServedByProxy: TranslatedMessage? = null

    /** Builds the `/busybar` command tree. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .executes(forPlayer(::status))
            .then(Commands.literal("status").requires(::isPlayer).executes(forPlayer(::status)))
            .then(Commands.literal("link").requires(::isPlayer).executes(forPlayer(::link)))
            .then(Commands.literal("unlink").requires(::isPlayer).executes(forPlayer(::unlink)))
            .then(
                Commands.literal("busy")
                    .requires(::isPlayer)
                    .then(Commands.literal("on").executes(forPlayer { setBusy(it, true) }))
                    .then(Commands.literal("off").executes(forPlayer { setBusy(it, false) }))
            )
            .then(
                Commands.literal("list").requires(::isAdmin).executes { ctx ->
                    unlessProxied(ctx.source.sender) { list(it) }
                    Command.SINGLE_SUCCESS
                }
            )
            .then(adminWithPlayer("clear", ::clear))
            .then(adminWithPlayer("revoke", ::revoke))
            .then(
                Commands.literal("rotatekey").requires(::isAdmin).executes { ctx ->
                    unlessProxied(ctx.source.sender, ::rotateKey)
                    Command.SINGLE_SUCCESS
                }
            )
    }

    private fun isPlayer(source: CommandSourceStack) = source.sender is Player

    private fun isAdmin(source: CommandSourceStack) = source.sender.hasPermission(adminPermission)

    /** Runs [action] for a player sender; anyone else gets the help text. */
    private fun forPlayer(action: (Player) -> Unit) = { ctx: CommandContext<CommandSourceStack> ->
        val sender = ctx.source.sender
        if (sender is Player) unlessProxied(sender, action) else printHelp(sender)
        Command.SINGLE_SUCCESS
    }

    /**
     * Runs [action] for [sender], unless a proxy manages BUSY Bars: its `/busybar` is the one that
     * counts, and players connected through it never reach this one.
     */
    private fun <S : CommandSender> unlessProxied(sender: S, action: (S) -> Unit) {
        if (busybar.proxied()) langServedByProxy!!.send(sender) else action(sender)
    }

    /** `/busybar <name> <player>`, for admins; works for offline players. */
    private fun adminWithPlayer(name: String, action: (CommandSender, OfflinePlayer) -> Unit) =
        Commands.literal(name)
            .requires(::isAdmin)
            .then(
                Commands.argument("player", OfflinePlayerArgumentType.offlinePlayer()).executes {
                    ctx ->
                    val target = ctx.getArgument("player", OfflinePlayer::class.java)
                    unlessProxied(ctx.source.sender) { action(it, target) }
                    Command.SINGLE_SUCCESS
                }
            )

    /**
     * Issues a fresh token and shows its pairing string once, click-to-copy. Without a known server
     * address, it explains that instead and keeps the current token.
     */
    private fun link(player: Player) {
        if (busybar.publicUrl() == null) return langNoPublicUrl!!.send(player)
        val pairing = busybar.pairingString(busybar.issueToken(player.uniqueId))!!
        langLinked!!.send(player)
        player.sendMessage(
            Component.text(pairing, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.copyToClipboard(pairing))
                .hoverEvent(HoverEvent.showText(langClickToCopy!!.format()))
        )
    }

    /**
     * Revokes the player's token and ends focus mode, which nothing could end once the Bar is gone.
     */
    private fun unlink(player: Player) {
        val id = player.uniqueId
        val wasBusy = busybar.presence.isBusy(id)
        if (wasBusy) busybar.presence.setBusy(id, false)
        if (busybar.revokeToken(id) || wasBusy) langUnlinked!!.send(player)
        else langNotLinked!!.send(player)
    }

    /** Shows whether a bridge is linked and connected. */
    private fun status(player: Player) {
        val id = player.uniqueId
        if (!busybar.isLinked(id)) return langNotLinked!!.send(player)
        langStatus!!.send(player, "§b${busybar.stream.connectedCount(id)}", focusState(id))
    }

    /** Switches focus mode from the game; the player's Bar follows through `presence.updated`. */
    private fun setBusy(player: Player, busy: Boolean) {
        busybar.presence.setBusy(player.uniqueId, busy)
        langBusySet!!.send(player, focusState(player.uniqueId))
    }

    /** Lists players with a token or focus mode on. */
    private fun list(sender: CommandSender) {
        val ids =
            (busybar.linkedPlayers() + busybar.presence.busyPlayers()).toSortedSet(
                compareBy { nameOf(it) }
            )
        if (ids.isEmpty()) return langListEmpty!!.send(sender)
        langListHeader!!.send(sender, "§b${ids.size}")
        for (id in ids) {
            val linked = if (busybar.isLinked(id)) langLinkedYes!!.str() else langLinkedNo!!.str()
            langListEntry!!.send(
                sender,
                nameOf(id),
                "§b${busybar.stream.connectedCount(id)}",
                focusState(id),
                linked,
            )
        }
    }

    /** Ends someone's focus mode. */
    private fun clear(sender: CommandSender, target: OfflinePlayer) {
        val id = target.uniqueId
        if (!busybar.presence.isBusy(id)) return langNothingToDo!!.send(sender, nameOf(id))
        busybar.presence.setBusy(id, false)
        langCleared!!.sendAndLog(sender, nameOf(id))
    }

    /** Revokes someone's token and ends their focus mode. */
    private fun revoke(sender: CommandSender, target: OfflinePlayer) {
        val id = target.uniqueId
        val wasBusy = busybar.presence.isBusy(id)
        if (wasBusy) busybar.presence.setBusy(id, false)
        if (!busybar.revokeToken(id) && !wasBusy) return langNothingToDo!!.send(sender, nameOf(id))
        langRevoked!!.sendAndLog(sender, nameOf(id))
    }

    /** Replaces the auto TLS key; every bridge has to be linked again. */
    private fun rotateKey(sender: CommandSender) {
        if (!busybar.rotateTlsKey()) return langKeyNotRotatable!!.send(sender)
        langKeyRotated!!.sendAndLog(sender)
    }

    private fun focusState(id: UUID) =
        if (busybar.presence.isBusy(id)) langBusyOn!!.str() else langBusyOff!!.str()

    private fun nameOf(id: UUID) = busybar.server.getOfflinePlayer(id).name ?: id.toString()
}
