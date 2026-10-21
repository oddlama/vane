package org.oddlama.vane.core.command

import org.bukkit.command.CommandSender

/** Represents a parsed command executor callback. */
interface Executor {
    /**
     * Executes a parsed command.
     *
     * @param command the owning command.
     * @param sender the command sender.
     * @param parsedArgs parsed argument list.
     * @return `true` when execution succeeded.
     */
    fun execute(
        command: Command<*>?,
        sender: CommandSender?,
        parsedArgs: MutableList<Any?>,
    ): Boolean
}
