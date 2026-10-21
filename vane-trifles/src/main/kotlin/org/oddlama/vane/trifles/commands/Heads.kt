package org.oddlama.vane.trifles.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil

@Name("heads")
/** Command that opens the player head library and handles optional currency payment. */
class Heads(context: Context<Trifles?>) :
    org.oddlama.vane.core.command.Command<Trifles?>(context, PermissionDefault.TRUE) {
    /** Material used as payment when buying heads from the selector menu. */
    @ConfigMaterial(def = Material.BONE, desc = "Currency material used to buy heads.")
    var configCurrency: Material? = null

    /** Price per head in the configured currency item. */
    @ConfigInt(def = 1, min = 0, desc = "Price (in currency) per head. Set to 0 for free heads.")
    var configPricePerHead: Int = 0

    /** Builds the `/heads` command tree. */
    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .requires { ctx: CommandSourceStack -> ctx.sender is Player }
            .then(help())
            .executes { ctx: CommandContext<CommandSourceStack> ->
                openHeadLibrary(ctx.getSource().sender as Player)
                Command.SINGLE_SUCCESS
            }
    }

    /** Opens the head selector menu for the given player. */
    private fun openHeadLibrary(player: Player) {
        val context = requireNotNull(getContext())
        MenuFactory.headSelector(
                context,
                player,
                {
                    selectedPlayer: Player?,
                    _: Menu?,
                    head: HeadMaterial?,
                    event: InventoryClickEvent? ->
                    val clickedEvent = event ?: return@headSelector Menu.ClickResult.INVALID_CLICK
                    val targetPlayer = selectedPlayer ?: return@headSelector Menu.ClickResult.ERROR
                    val selectedHead = head ?: return@headSelector Menu.ClickResult.ERROR

                    val amount =
                        when (clickedEvent.click) {
                            ClickType.NUMBER_KEY -> clickedEvent.hotbarButton + 1
                            ClickType.LEFT -> 1
                            ClickType.RIGHT -> 32
                            ClickType.MIDDLE,
                            ClickType.SHIFT_LEFT -> 64
                            ClickType.SHIFT_RIGHT -> 16
                            else -> return@headSelector Menu.ClickResult.INVALID_CLICK
                        }

                    // Charge configured currency before granting heads.
                    if (
                        configPricePerHead > 0 &&
                            !PlayerUtil.takeItems(
                                targetPlayer,
                                ItemStack(
                                    requireNotNull(configCurrency),
                                    configPricePerHead * amount,
                                ),
                            )
                    ) {
                        return@headSelector Menu.ClickResult.ERROR
                    }

                    PlayerUtil.giveItems(targetPlayer, selectedHead.item(), amount)
                    Menu.ClickResult.SUCCESS
                },
                { _: Player? -> },
            )
            .open(player)
    }
}
