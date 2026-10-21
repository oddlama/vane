package org.oddlama.vane.permissions.argumentTypes

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture

/**
 * Brigadier argument type that validates and suggests configured permission group names.
 *
 * @property permissionGroups available groups used for command suggestions.
 */
class PermissionGroupArgumentType
private constructor(private val permissionGroups: Map<String, Set<String>>) :
    CustomArgumentType.Converted<String, String> {
    /** Returns the parsed permission group name unchanged. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: String): String {
        return nativeType
    }

    /** Uses Brigadier string parsing for the underlying native type. */
    override fun getNativeType(): ArgumentType<String> = StringArgumentType.string()

    /** Suggests known group names that contain the currently typed input. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        permissionGroups.keys
            .asSequence()
            .filter { remaining.isBlank() || it.contains(remaining) }
            .forEach(builder::suggest)
        return builder.buildFuture()
    }

    /** Static factories for permission group argument creation. */
    companion object {
        /**
         * Factory for creating a permission group argument backed by current group configuration.
         */
        fun permissionGroup(
            permissionGroups: MutableMap<String, MutableSet<String>>
        ): PermissionGroupArgumentType = PermissionGroupArgumentType(permissionGroups)
    }
}
