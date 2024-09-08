package org.oddlama.vane.core.command.argumentType;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

public class OfflinePlayerArgumentType implements CustomArgumentType.Converted<OfflinePlayer, String>{

    public static @NotNull OfflinePlayerArgumentType offlinePlayer() {
        return new OfflinePlayerArgumentType();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public @NotNull OfflinePlayer convert(@NotNull String nativeType) throws CommandSyntaxException {
        return Bukkit.getOfflinePlayer(nativeType);
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
            @NotNull SuggestionsBuilder builder) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        Stream<String> stream = Arrays.stream(players).map(p -> p.getName()).filter(p -> p != null);
        if(!builder.getRemaining().isBlank()) {
            stream = stream.filter(player -> player.contains(builder.getRemaining()));
        }
        stream.forEach(builder::suggest);
        return builder.buildFuture();
    }    
}
