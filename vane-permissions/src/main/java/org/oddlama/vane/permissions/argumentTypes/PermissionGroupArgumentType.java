package org.oddlama.vane.permissions.argumentTypes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

public class PermissionGroupArgumentType implements CustomArgumentType.Converted<String, String> {

    Map<String, Set<String>> permission_groups;

    public static @NotNull PermissionGroupArgumentType permissionGroup(Map<String, Set<String>> permission_groups) {
        return new PermissionGroupArgumentType(permission_groups);
    }

    private PermissionGroupArgumentType(Map<String, Set<String>> permission_groups) {
        this.permission_groups = permission_groups;
    }

    @Override
    public @NotNull String convert(@NotNull String nativeType) throws CommandSyntaxException {
        return nativeType;
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
            @NotNull SuggestionsBuilder builder) {
        Stream<String> stream = permission_groups.keySet().stream();
        if(!builder.getRemaining().isBlank()) {
            stream = stream.filter(group -> group.contains(builder.getRemaining()));
        }
        stream.forEach(builder::suggest);
        return builder.buildFuture();
    }

    
}
