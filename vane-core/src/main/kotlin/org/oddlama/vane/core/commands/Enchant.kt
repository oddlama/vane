package org.oddlama.vane.core.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.command.argumentType.EnchantmentFilterArgumentType.Companion.enchantmentFilter
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/**
 * Command for applying enchantments to the held item.
 *
 * @param context command context.
 */
@Name("enchant")
class Enchant(context: Context<Core?>) : org.oddlama.vane.core.command.Command<Core?>(context) {
    /** Message shown when the requested level is below the enchantment minimum. */
    @LangMessage private val langLevelTooLow: TranslatedMessage? = null

    /** Message shown when the requested level exceeds the enchantment maximum. */
    @LangMessage private val langLevelTooHigh: TranslatedMessage? = null

    /** Message shown when the enchantment is invalid for the current item. */
    @LangMessage private val langInvalidEnchantment: TranslatedMessage? = null

    /** Builds the brigadier command tree for `/enchant`. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> =
        super.getCommandBase()
            .requires { it.sender is Player }
            .then(help())
            .then(
                Commands.argument("enchantment", enchantmentFilter())
                    .executes { ctx ->
                        enchantCurrentItem(ctx.source.sender as Player, enchantment(ctx)!!, 1)
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.argument("level", IntegerArgumentType.integer(1)).executes { ctx ->
                            enchantCurrentItem(
                                ctx.source.sender as Player,
                                enchantment(ctx)!!,
                                ctx.getArgument("level", Int::class.java),
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
            )

    /** Resolves the parsed enchantment argument from command context. */
    private fun enchantment(ctx: CommandContext<CommandSourceStack>): Enchantment? =
        ctx.getArgument("enchantment", Enchantment::class.java)

    /** Applies an enchantment to the player's held item. */
    private fun enchantCurrentItem(player: Player, enchantment: Enchantment, level: Int) {
        when {
            level < enchantment.startLevel -> {
                langLevelTooLow!!.send(player, "§b$level", "§a${enchantment.startLevel}")
                return
            }

            level > enchantment.maxLevel -> {
                langLevelTooHigh!!.send(player, "§b$level", "§a${enchantment.maxLevel}")
                return
            }
        }

        var itemStack = player.equipment.itemInMainHand
        if (itemStack.type == Material.AIR) {
            langInvalidEnchantment!!.send(player, "§b${enchantment.key}", "§a${itemStack.type.key}")
            return
        }

        try {
            if (itemStack.type == Material.BOOK) {
                itemStack = itemStack.withType(Material.ENCHANTED_BOOK)
            }

            if (itemStack.type == Material.ENCHANTED_BOOK) {
                val meta = itemStack.itemMeta as EnchantmentStorageMeta
                meta.addStoredEnchant(enchantment, level, false)
                itemStack.itemMeta = meta
            } else {
                itemStack.addEnchantment(enchantment, level)
            }

            module!!.enchantmentManager?.updateEnchantedItem(itemStack)
        } catch (_: Exception) {
            langInvalidEnchantment!!.send(player, "§b${enchantment.key}", "§a${itemStack.type.key}")
        }
    }
}
