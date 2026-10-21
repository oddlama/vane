package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture
import org.oddlama.vane.core.command.enums.WeatherValue

/** Brigadier argument type for [WeatherValue] presets. */
class WeatherArgumentType : CustomArgumentType.Converted<WeatherValue, String> {
    /** Returns the native brigadier argument type. */
    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

    /** Converts a string token into a [WeatherValue]. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): WeatherValue =
        WeatherValue.valueOf(nativeType.replaceFirstChar { it.uppercaseChar() })

    /** Builds completion suggestions from [WeatherValue] display names. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        WeatherValue.entries
            .filter { remaining.isBlank() || it.displayName.contains(remaining) }
            .forEach { builder.suggest(it.displayName) }
        return builder.buildFuture()
    }

    /** Factory methods for [WeatherArgumentType]. */
    companion object {
        /** Creates a new weather argument type. */
        fun weather() = WeatherArgumentType()
    }
}
