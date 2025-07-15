package org.oddlama.vane.core.command.argumentType;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnchantmentFilterArgumentType implements CustomArgumentType.Converted<Enchantment, Enchantment> {

    ItemStack item;

    public static EnchantmentFilterArgumentType enchantmentFilter() {
        return new EnchantmentFilterArgumentType();
    }

    @Override
    public @NotNull ArgumentType<Enchantment> getNativeType() {
        return ArgumentTypes.resource(RegistryKey.ENCHANTMENT);
    }

    @Override
    public @NotNull Enchantment convert(@NotNull Enchantment nativeType) throws CommandSyntaxException {
        return nativeType;
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(
        @NotNull CommandContext<S> context,
        @NotNull SuggestionsBuilder builder
    ) {
        CommandSourceStack stack = (CommandSourceStack) context.getSource();
        ItemStack item = ((Player) stack.getSender()).getInventory().getItemInMainHand();

        Stream<Enchantment> compatibleEnchantments = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ENCHANTMENT)
            .stream();
        if (item.getType() != Material.BOOK && item.getType() != Material.ENCHANTED_BOOK) {
            compatibleEnchantments = compatibleEnchantments.filter(ench -> ench.canEnchantItem(item));
        }

        Stream<String> stream = compatibleEnchantments.map(ench -> ench.getKey().asString());
        if (!builder.getRemaining().isBlank()) {
            stream = stream.filter(ench -> ench.contains(builder.getRemainingLowerCase()));
        }

        stream.forEach(builder::suggest);
        return builder.buildFuture();
    }
}
