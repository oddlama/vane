package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

/** Brigadier argument type that resolves offline players by name. */
class OfflinePlayerArgumentType : CustomArgumentType.Converted<OfflinePlayer, String> {

    /** Returns the native brigadier argument type. */
    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

    /** Converts a player name into an offline player entry. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): OfflinePlayer =
        Bukkit.getOfflinePlayers().firstOrNull { nativeType.equals(it.name, ignoreCase = true) }
            ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create()

    /** Builds completion suggestions from known offline player names. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        Bukkit.getOfflinePlayers()
            .asSequence()
            .mapNotNull { it.name }
            .filter { remaining.isBlank() || it.contains(remaining, ignoreCase = true) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    /** Factory methods for [OfflinePlayerArgumentType]. */
    companion object {
        @JvmStatic
        /** Creates a new offline-player argument type. */
        fun offlinePlayer(): OfflinePlayerArgumentType = OfflinePlayerArgumentType()
    }
}
