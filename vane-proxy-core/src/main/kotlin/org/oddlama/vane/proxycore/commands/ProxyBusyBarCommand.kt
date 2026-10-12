package org.oddlama.vane.proxycore.commands

import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.VaneProxyPlugin
import java.util.*

/**
 * `/busybar` on the proxy: players link, unlink and inspect their BUSY Bar bridge and switch focus
 * mode; admins list linked players, clear or revoke someone else's, and replace the TLS key.
 *
 * Players need no permission; the admin subcommands need [ADMIN_PERMISSION] on the proxy and are
 * always allowed from the console. The proxy has no translation files, so messages are English.
 *
 * @constructor Creates the command.
 */
class ProxyBusyBarCommand(plugin: VaneProxyPlugin) : ProxyCommand(null, plugin) {
    override fun execute(sender: ProxyCommandSender?, args: Array<String?>?) {
        sender ?: return
        val busyBar = plugin.busyBar
        if (!busyBar.running) return sender.sendMessage(NOT_ENABLED)
        val player = (sender as? ProxyPlayer)?.uniqueId
        val words = args.orEmpty().filterNotNull()

        fun admin(action: () -> Unit) = if (isAdmin(sender)) action() else sender.sendMessage(NO_PERMISSION)
        fun self(action: (UUID) -> Unit) = if (player != null) action(player) else sender.sendMessage(HELP)
        fun target(action: (UUID) -> Unit) = admin {
            val name = words.getOrNull(1) ?: return@admin sender.sendMessage(HELP)
            val id = plugin.proxy.players.orEmpty().firstOrNull { it?.name.equals(name, ignoreCase = true) }?.uniqueId
                ?: busyBar.playerNamed(name)
                ?: return@admin sender.sendMessage("§7Unknown player §b$name§7.")
            action(id)
        }

        when (words.firstOrNull()?.lowercase(Locale.ROOT)) {
            null, "status" -> self { status(sender, it) }
            "link" -> self { link(sender, it) }
            "unlink" -> self { unlink(sender, it) }
            "busy" -> when (words.getOrNull(1)?.lowercase(Locale.ROOT)) {
                "on" -> self { setBusy(sender, it, true) }
                "off" -> self { setBusy(sender, it, false) }
                else -> sender.sendMessage(HELP)
            }
            "list" -> admin { list(sender) }
            "clear" -> target { clear(sender, it) }
            "revoke" -> target { revoke(sender, it) }
            "rotatekey" -> admin { rotateKey(sender) }
            else -> sender.sendMessage(HELP)
        }
    }

    /**
     * Completes subcommand names, and player names after `clear` and `revoke`.
     *
     * @param sender command sender.
     * @param args arguments typed so far, the last one possibly empty.
     * @return matching completions.
     */
    fun suggest(sender: ProxyCommandSender, args: List<String>): List<String> {
        val options = when (args.size) {
            0, 1 -> SELF_SUBCOMMANDS + if (isAdmin(sender)) ADMIN_SUBCOMMANDS else emptyList()
            2 -> when (args[0].lowercase(Locale.ROOT)) {
                "busy" -> listOf("on", "off")
                "clear", "revoke" -> if (isAdmin(sender)) plugin.proxy.players.orEmpty().mapNotNull { it?.name } else emptyList()
                else -> emptyList()
            }
            else -> emptyList()
        }
        val typed = args.lastOrNull().orEmpty()
        return options.filter { it.startsWith(typed, ignoreCase = true) }
    }

    private fun isAdmin(sender: ProxyCommandSender): Boolean =
        sender !is ProxyPlayer || plugin.proxy.hasPermission(sender.uniqueId, ADMIN_PERMISSION)

    /** Issues a fresh token and shows its pairing string once, click-to-copy. */
    private fun link(sender: ProxyCommandSender, player: UUID) {
        val pairing = plugin.busyBar.link(player) ?: return sender.sendMessage(NO_PUBLIC_URL)
        sender.sendMessage("§7Paste the line below into your bridge. It holds your token and is shown only once.")
        sender.sendCopyable("§b$pairing", pairing, "§7Click to copy")
    }

    /** Revokes the player's token and ends focus mode, which nothing could end once the Bar is gone. */
    private fun unlink(sender: ProxyCommandSender, player: UUID) {
        val busyBar = plugin.busyBar
        val wasBusy = busyBar.isBusy(player)
        if (wasBusy) busyBar.setBusy(player, false)
        sender.sendMessage(if (busyBar.unlink(player) || wasBusy) "§7Your BUSY Bar token was revoked." else NOT_LINKED)
    }

    private fun status(sender: ProxyCommandSender, player: UUID) {
        val busyBar = plugin.busyBar
        if (!busyBar.isLinked(player)) return sender.sendMessage(NOT_LINKED)
        sender.sendMessage("§7BUSY Bar linked. Connected bridges: §b${busyBar.connectedCount(player)}§7. Focus mode: ${focus(player)}")
    }

    private fun setBusy(sender: ProxyCommandSender, player: UUID, busy: Boolean) {
        plugin.busyBar.setBusy(player, busy)
        sender.sendMessage("§7Focus mode: ${focus(player)}")
    }

    private fun list(sender: ProxyCommandSender) {
        val busyBar = plugin.busyBar
        val players = busyBar.knownPlayers().sortedBy { busyBar.nameOf(it).lowercase(Locale.ROOT) }
        if (players.isEmpty()) return sender.sendMessage("§7No player has a BUSY Bar linked or focus mode on.")
        sender.sendMessage("§7BUSY Bar players (§b${players.size}§7):")
        for (player in players) {
            val linked = if (busyBar.isLinked(player)) "§alinked" else "§7not linked"
            sender.sendMessage(
                "§7- §b${busyBar.nameOf(player)}§7 · bridges: §b${busyBar.connectedCount(player)}§7 · " +
                    "focus: ${focus(player)}§7 · $linked"
            )
        }
    }

    private fun clear(sender: ProxyCommandSender, player: UUID) {
        val busyBar = plugin.busyBar
        val name = busyBar.nameOf(player)
        if (!busyBar.isBusy(player)) return sender.sendMessage(nothingToDo(name))
        busyBar.setBusy(player, false)
        sender.sendMessage("§7Focus mode turned off for §b$name§7.")
    }

    private fun revoke(sender: ProxyCommandSender, player: UUID) {
        val busyBar = plugin.busyBar
        val name = busyBar.nameOf(player)
        val wasBusy = busyBar.isBusy(player)
        if (wasBusy) busyBar.setBusy(player, false)
        if (!busyBar.unlink(player) && !wasBusy) return sender.sendMessage(nothingToDo(name))
        sender.sendMessage("§7Revoked the BUSY Bar token of §b$name§7.")
    }

    private fun rotateKey(sender: ProxyCommandSender) {
        if (!plugin.busyBar.rotateTlsKey()) {
            return sender.sendMessage("§7Only possible with §bTls = \"auto\"§7. This proxy uses its own certificate or no TLS.")
        }
        sender.sendMessage("§7Generated a new TLS key. Every player with a BUSY Bar has to run §b/busybar link§7 again.")
    }

    private fun focus(player: UUID) = if (plugin.busyBar.isBusy(player)) "§con" else "§aoff"

    private fun nothingToDo(name: String) = "§b$name§7 has nothing to clear: no BUSY Bar token or focus mode."

    companion object {
        /** Required for `list`, `clear`, `revoke` and `rotatekey`. */
        const val ADMIN_PERMISSION = "vane.busybar.admin"

        private val SELF_SUBCOMMANDS = listOf("status", "link", "unlink", "busy")
        private val ADMIN_SUBCOMMANDS = listOf("list", "clear", "revoke", "rotatekey")

        private const val NOT_ENABLED = "§7BUSY Bar support is not enabled on this proxy."
        private const val NO_PERMISSION = "§cYou may not manage other players' BUSY Bars."
        private const val NOT_LINKED = "§7No BUSY Bar is linked. Use §b/busybar link§7 to create a token."
        private const val NO_PUBLIC_URL =
            "§7This proxy has no address set for BUSY Bar bridges yet. Ask an admin to set §bBusyBar.PublicUrl§7 in the vane-velocity config."

        private val HELP = """
            §7> §3/busybar §7[§astatus§7] §f- Shows whether your bridge is connected
            §7> §3/busybar §alink §f- Creates a token for your BUSY Bar bridge, replacing the old one
            §7> §3/busybar §aunlink §f- Revokes your token and turns focus mode off
            §7> §3/busybar §abusy §7<§aon§7|§aoff§7> §f- Turns focus mode on or off
            §7> §3/busybar §alist §f- (admin) Lists players with a linked BUSY Bar or focus mode on
            §7> §3/busybar §aclear §7<§bplayer§7> §f- (admin) Turns off a player's focus mode
            §7> §3/busybar §arevoke §7<§bplayer§7> §f- (admin) Revokes a player's token
            §7> §3/busybar §arotatekey §f- (admin) Replaces the TLS key; everyone has to link again
        """.trimIndent()
    }
}
