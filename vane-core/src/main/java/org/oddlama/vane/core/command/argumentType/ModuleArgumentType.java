package org.oddlama.vane.core.command.argumentType;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.module.Module;

public class ModuleArgumentType implements CustomArgumentType.Converted<Module<?>, String> {

    Core core;

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    public static @NotNull ModuleArgumentType module(Core module) {
        return new ModuleArgumentType().setCore(module);
    }

    private ModuleArgumentType setCore(Core module) {
        this.core = module.core;
        return this;
    }

    @Override
    public @NotNull Module<?> convert(@NotNull String nativeType) throws CommandSyntaxException {
        return core
            .get_modules()
            .stream()
            .filter(module -> module.get_name().equalsIgnoreCase(nativeType))
            .findFirst()
            .orElseThrow();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(
        @NotNull CommandContext<S> context,
        @NotNull SuggestionsBuilder builder
    ) {
        Stream<String> stream = core.get_modules().stream().map(Module::get_name);
        if (!builder.getRemaining().isBlank()) {
            stream = stream.filter(module -> module.contains(builder.getRemainingLowerCase()));
        }

        stream.forEach(module -> builder.suggest(module));
        return builder.buildFuture();
    }
}
