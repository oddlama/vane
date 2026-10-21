package org.oddlama.vane.enchantments.enchantments

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.data.CooldownData
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.StorageUtil

/**
 * Soulbound is a rare enchantment that prevents items from being dropped unless certain conditions
 * are met. It has a cooldown period after an item is dropped, during which no other soulbound items
 * can be dropped.
 */
@VaneEnchantment(name = "soulbound", rarity = Rarity.RARE, treasure = true, allowCustom = true)
class Soulbound(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * The cooldown period (in milliseconds) that prevents dropping soulbound items immediately
     * after another drop.
     */
    @ConfigLong(
        def = 2000,
        min = 0,
        desc =
            "Window to allow Soulbound item drop immediately after a previous drop in milliseconds",
    )
    var configCooldown: Long = 0

    /** Tracks drop cooldown state for soulbound item interactions. */
    private var dropCooldown = CooldownData(IGNORE_SOULBOUND_DROP, configCooldown)

    /**
     * Warning message shown when a player attempts to drop a soulbound item before the cooldown
     * period has expired.
     */
    @LangMessage var langDropLockWarning: TranslatedMessage? = null

    /** Notification message sent to a player when a soulbound item is dropped successfully. */
    @LangMessage var langDroppedNotification: TranslatedMessage? = null

    /**
     * Message indicating the remaining cooldown time before a soulbound item can be dropped again.
     */
    @LangMessage var langDropCooldown: TranslatedMessage? = null

    /**
     * Called when the configuration is changed. Updates the cooldown data for dropping soulbound
     * items.
     */
    public override fun onConfigChange() {
        super.onConfigChange()
        dropCooldown = CooldownData(IGNORE_SOULBOUND_DROP, configCooldown)
    }

    /**
     * Defines the default recipes involving the soulbound enchantment, including the ingredients
     * and the resulting item.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("CQC", "OBE", "RGT")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('C', Material.IRON_CHAIN)
                .setIngredient('Q', Material.WRITABLE_BOOK)
                .setIngredient('O', Material.BONE)
                .setIngredient('R', "minecraft:enchanted_book#enchants{minecraft:binding_curse*1}")
                .setIngredient('G', Material.GHAST_TEAR)
                .setIngredient('T', Material.TOTEM_OF_UNDYING)
                .setIngredient('E', Material.ENDER_EYE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Defines the default loot tables for the soulbound enchantment, specifying where soulbound
     * items can be found in the game world.
     */
    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 15, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Applies a custom display format to soulbound items, changing their color in the user
     * interface.
     */
    override fun applyDisplayFormat(component: Component): Component {
        return component.color(NamedTextColor.DARK_GRAY)
    }

    /**
     * Event handler that prevents players from dropping soulbound items on death, instead keeping
     * these items in their inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val keepItems = event.itemsToKeep

        // Keep all soulbound items
        val it = event.drops.iterator()
        while (it.hasNext()) {
            val drop = it.next()
            if (isSoulbound(drop)) {
                keepItems.add(drop)
                it.remove()
            }
        }
    }

    /**
     * Event handler that manages the interaction between the player's inventory and soulbound
     * items, specifically preventing dropping soulbound items through inventory actions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInventoryCheck(event: InventoryClickEvent) {
        if (!isSoulbound(event.cursor)) return
        if (
            event.action == InventoryAction.DROP_ALL_CURSOR ||
                event.action == InventoryAction.DROP_ONE_CURSOR
        ) {
            val tooSlow = dropCooldown.peekCooldown(event.cursor.itemMeta)
            if (tooSlow) {
                // Dropped too slowly, refresh and cancel
                val meta = event.cursor.itemMeta
                dropCooldown.checkOrUpdateCooldown(meta)
                event.cursor.itemMeta = meta
                langDropCooldown?.sendActionBar(event.whoClicked)
                event.result = Event.Result.DENY
                return
            }
            // else allow as normal
        }
    }

    /**
     * Event handler that prevents players from dropping soulbound items from their inventory, with
     * specific behavior depending on the player's inventory state.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        // A player cannot drop soulbound items.
        // Prevents yeeting your best sword out of existence.
        // (It's okay to put them into chests.)
        val droppedItem = event.itemDrop.itemStack
        if (isSoulbound(droppedItem)) {
            val tooSlow = dropCooldown.peekCooldown(droppedItem.itemMeta)
            if (!tooSlow) {
                val meta = droppedItem.itemMeta
                dropCooldown.clear(meta)
                droppedItem.itemMeta = meta
                langDroppedNotification?.send(event.player, droppedItem.displayName())
                return
            }
            val inventory = event.player.inventory
            if (inventory.firstEmpty() != -1) {
                // We still have space in the inventory, so the player tried to drop it with Q.
                event.isCancelled = true
                langDropLockWarning?.sendActionBar(
                    event.player,
                    event.itemDrop.itemStack.displayName(),
                )
            } else {
                // Inventory is full (e.g., when exiting crafting table with soulbound item in it)
                // so we drop the first non-soulbound item (if any) instead.
                val it = inventory.iterator()
                var nonSoulboundItem: ItemStack? = null
                var nonSoulboundItemSlot = 0
                while (it.hasNext()) {
                    val item = it.next()
                    if (item.getEnchantmentLevel(requireNotNull(bukkit())) == 0) {
                        nonSoulboundItem = item
                        break
                    }

                    ++nonSoulboundItemSlot
                }

                if (nonSoulboundItem == null) {
                    // We can't prevent dropping a soulbound item.
                    // Well, that sucks.
                    return
                }

                // Drop the other item
                val player = event.player
                inventory.setItem(nonSoulboundItemSlot, droppedItem)
                player.location.world.dropItem(player.location, nonSoulboundItem)
                langDropLockWarning?.sendActionBar(player, event.itemDrop.itemStack.displayName())
                event.isCancelled = true
            }
        }
    }

    /**
     * Checks if an item is soulbound by examining its enchantment level.
     *
     * @param droppedItem The item to check.
     * @return True if the item is soulbound, false otherwise.
     */
    private fun isSoulbound(droppedItem: ItemStack): Boolean {
        return droppedItem.getEnchantmentLevel(requireNotNull(bukkit())) > 0
    }

    /** Constants used by Soulbound metadata handling. */
    companion object {
        /** Key used to identify the cooldown data for ignoring soulbound drop restrictions. */
        private val IGNORE_SOULBOUND_DROP =
            StorageUtil.namespacedKey(
                "vane_enchantments",
                "ignore_soulbound_drop",
            )
    }
}
