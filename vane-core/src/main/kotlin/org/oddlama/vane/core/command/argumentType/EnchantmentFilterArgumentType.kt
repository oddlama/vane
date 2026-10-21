package org.oddlama.vane.core.command.argumentType

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import java.util.concurrent.CompletableFuture
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

/** Brigadier argument type for enchantments with sender-item-aware completion filtering. */
class EnchantmentFilterArgumentType : CustomArgumentType.Converted<Enchantment, Enchantment> {

    /** Returns the native brigadier argument type. */
    override fun getNativeType(): ArgumentType<Enchantment> =
        ArgumentTypes.resource(RegistryKey.ENCHANTMENT)

    /** Returns the already parsed native enchantment value. */
    @Throws(CommandSyntaxException::class)
    override fun convert(nativeType: Enchantment): Enchantment = nativeType

    /** Suggests enchantments applicable to the sender's held item. */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val stack = context.source as CommandSourceStack
        val item = (stack.sender as Player).inventory.itemInMainHand

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
        val remaining = builder.remainingLowerCase

        registry
            .asSequence()
            .filter {
                item.type == Material.BOOK ||
                    item.type == Material.ENCHANTED_BOOK ||
                    it.canEnchantItem(item)
            }
            .map { it.key.asString() }
            .filter { remaining.isBlank() || it.contains(remaining) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    /** Factory methods for [EnchantmentFilterArgumentType]. */
    companion object {
        @JvmStatic
        /** Creates a new enchantment filter argument type. */
        fun enchantmentFilter(): EnchantmentFilterArgumentType = EnchantmentFilterArgumentType()
    }
}
