package org.oddlama.vane.core.command.argumentType;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.command.enums.WeatherValue;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

public class WeatherArgumentType implements CustomArgumentType.Converted<WeatherValue, String>{

    public static WeatherArgumentType weather() {
        return new WeatherArgumentType();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public @NotNull WeatherValue convert(@NotNull String nativeType) throws CommandSyntaxException {
        return WeatherValue.valueOf(nativeType);
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context,
            @NotNull SuggestionsBuilder builder) {
        Stream<String> stream = Arrays.stream(WeatherValue.values()).map(time -> time.name());
        if(!builder.getRemaining().isBlank()) {
            stream = stream.filter(weatherName -> weatherName.contains(builder.getRemaining()));
        }
        stream.forEach(builder::suggest);
        return builder.buildFuture();
    } 
    
    
}
