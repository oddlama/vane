package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.item.api.CustomItem

/**
 * Brigadier argument type that resolves vane custom items by namespaced key.
 *
 * @param module core module providing the custom item registry.
 */
class CustomItemArgumentType private constructor(private val module: Core) :
    CustomArgumentType.Converted<CustomItem, NamespacedKey> {

    /** Returns the native brigadier argument type. */
    override fun getNativeType(): ArgumentType<NamespacedKey> = ArgumentTypes.namespacedKey()

    /** Converts a namespaced key into a registered custom item. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: NamespacedKey): CustomItem =
        module.itemRegistry()?.all()?.firstOrNull { it.key() == nativeType }
            ?: throw SimpleCommandExceptionType(
                    MessageComponentSerializer.message()
                        .serialize(Component.text("Unknown custom item: $nativeType"))
                )
                .create()

    /** Builds completion suggestions for registered custom items. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        module
            .itemRegistry()
            ?.all()
            ?.asSequence()
            ?.filter { remaining.isBlank() || it.key().toString().lowercase().contains(remaining) }
            ?.forEach { item ->
                val key = item.key().toString()
                val name = item.displayName()
                if (name != null)
                    builder.suggest(key, MessageComponentSerializer.message().serialize(name))
                else builder.suggest(key)
            }
        return builder.buildFuture()
    }

    /** Factory methods for [CustomItemArgumentType]. */
    companion object {
        /** Creates a custom item argument type for the given core module. */
        fun customItem(module: Core): CustomItemArgumentType = CustomItemArgumentType(module)
    }
}
