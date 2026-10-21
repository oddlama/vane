package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.text.Component
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.module.Module

/**
 * Brigadier argument type that resolves loaded vane modules by annotation name.
 *
 * @param core core module used to access loaded modules.
 */
class ModuleArgumentType private constructor(private val core: Core) :
    CustomArgumentType.Converted<Module<*>, String> {

    /** Returns the native brigadier argument type. */
    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

    /** Converts a module name into a loaded module instance. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): Module<*> =
        core.modules.asSequence().filterNotNull().firstOrNull {
            it.annotationName.equals(nativeType, ignoreCase = true)
        }
            ?: throw SimpleCommandExceptionType(
                    MessageComponentSerializer.message()
                        .serialize(Component.text("Unknown module: $nativeType"))
                )
                .create()

    /** Builds completion suggestions from loaded module names. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        core.modules
            .asSequence()
            .filterNotNull()
            .map { it.annotationName.lowercase() }
            .filter { remaining.isBlank() || it.contains(remaining) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    /** Factory methods for [ModuleArgumentType]. */
    companion object {
        @JvmStatic
        /** Creates a module argument type for the given core module. */
        fun module(core: Core): ModuleArgumentType = ModuleArgumentType(core)
    }
}
