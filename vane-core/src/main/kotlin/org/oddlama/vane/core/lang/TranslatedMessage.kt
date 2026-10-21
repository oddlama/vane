package org.oddlama.vane.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.oddlama.vane.core.module.Module

/**
 * Represents a translatable message with formatting and send/broadcast helpers.
 *
 * @param module the owning module.
 * @param key the translation key.
 * @param defaultTranslation the fallback translation format string.
 */
class TranslatedMessage(
    /** Owning module. */
    private val module: Module<*>,
    /** Translation key. */
    private val key: String,
    /** Fallback translation format string. */
    private val defaultTranslation: String,
) {
    /** Returns the translation key. */
    fun key(): String = key

    /** Formats this message as a plain string using fallback translations. */
    fun str(vararg args: Any?): String =
        try {
            String.format(defaultTranslation, *stringifyArgsForStr(key, args))
        } catch (e: Exception) {
            throw RuntimeException("Error while formatting message '$key'", e)
        }

    /** Formats this message and deserializes it as a legacy component. */
    fun strComponent(vararg args: Any?): Component =
        LegacyComponentSerializer.legacySection().deserialize(str(*args))

    /** Formats this message as a component, preferring client-side translations when enabled. */
    fun format(vararg args: Any?): Component {
        if (module.core?.configClientSideTranslations != true) return strComponent(*args)
        val list = args.map { o ->
            when (o) {
                is ComponentLike -> o
                is String -> LegacyComponentSerializer.legacySection().deserialize(o)
                else ->
                    throw RuntimeException(
                        "Error while formatting message '$key', got invalid argument $o"
                    )
            }
        }
        return Component.translatable(key, list)
    }

    /** Broadcasts this message to all online players. */
    fun broadcastServerPlayers(vararg args: Any?) {
        val component = format(*args)
        module.server.onlinePlayers.forEach { it.sendMessage(component) }
    }

    /** Broadcasts this message to all players and logs it to console. */
    fun broadcastServer(vararg args: Any?) {
        val component = format(*args)
        module.server.onlinePlayers.forEach { it.sendMessage(component) }
        module.clog.info(Component.text("[broadcast] ").append(strComponent(*args)))
    }

    /** Broadcasts this message to all players in a world. */
    fun broadcastWorld(world: World, vararg args: Any?) {
        val component = format(*args)
        world.players.forEach { it.sendMessage(component) }
    }

    /** Broadcasts this message as an action bar to all players in a world. */
    fun broadcastWorldActionBar(world: World, vararg args: Any?) {
        val component = format(*args)
        world.players.forEach { it.sendActionBar(component) }
    }

    /** Sends this message to a sender, defaulting to console formatting for console targets. */
    fun send(sender: CommandSender?, vararg args: Any?) {
        if (sender == null || sender === module.server.consoleSender) {
            module.server.consoleSender.sendMessage(strComponent(*args))
        } else {
            sender.sendMessage(format(*args))
        }
    }

    /** Sends this message as an action bar to a non-console sender. */
    fun sendActionBar(sender: CommandSender?, vararg args: Any?) {
        if (sender != null && sender !== module.server.consoleSender) {
            sender.sendActionBar(format(*args))
        }
    }

    /** Logs this message and sends it to a non-console sender. */
    fun sendAndLog(sender: CommandSender?, vararg args: Any?) {
        module.clog.info(strComponent(*args))
        if (sender != null && sender !== module.server.consoleSender) {
            sender.sendMessage(format(*args))
        }
    }
}
