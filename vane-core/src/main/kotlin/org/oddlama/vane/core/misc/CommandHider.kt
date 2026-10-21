package org.oddlama.vane.core.misc

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/**
 * Replaces permission-denied command feedback with vanilla unknown-command output.
 *
 * @param context listener context.
 */
class CommandHider(context: Context<Core?>) :
    Listener<Core?>(
        context.group(
            "HideCommands",
            "Hide error messages for all commands for which a player has no permission, by displaying the default unknown command message instead.",
        )
    ) {
    /** Returns whether a command preprocess event should be allowed to continue. */
    private fun allowCommandEvent(message: String, player: Player): Boolean {
        val trimmed = message.trim()
        if (!trimmed.startsWith("/")) return false

        val raw = trimmed.substring(1)
        val id = raw.substringBefore(' ')

        val command = module!!.server.commandMap.knownCommands[id] ?: return true
        return command.testPermissionSilent(player)
    }

    /** Handles command preprocessing and hides permission-denied command hints. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        if (!allowCommandEvent(event.message, event.player)) {
            event.player.sendMessage("Unknown command. Type \"/help\" for help.")
            event.isCancelled = true
        }
    }
}
