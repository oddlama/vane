package org.oddlama.vane.util

import com.destroystokyo.paper.profile.ProfileProperty
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.world.item.Item
import org.bukkit.*
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.util.Nms.creativeTabId
import org.oddlama.vane.util.Nms.itemHandle
import org.oddlama.vane.util.Nms.playerHandle
import org.oddlama.vane.util.Nms.worldHandle

/** Utility helpers for item creation, naming, parsing, and comparison. */
object ItemUtil {
    /** Stable synthetic owner UUID used for generated skull textures. */
    private val SKULL_OWNER: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    /** Damages an item in hand using NMS break handling. */
    @JvmStatic
    fun damageItem(player: Player, itemStack: ItemStack, amount: Int) {
        if (player.gameMode == GameMode.CREATIVE) return
        if (amount <= 0) return
        val handle = itemHandle(itemStack) ?: return
        handle.hurtAndBreak(amount, worldHandle(player.world), playerHandle(player)) { _ ->
            player.broadcastSlotBreak(EquipmentSlot.HAND)
            itemStack.subtract()
        }
    }

    /** Extracts plain-text display name from an item stack. */
    fun nameOf(item: ItemStack?): String {
        if (item == null || !item.hasItemMeta()) return ""
        val meta = item.itemMeta
        if (!meta.hasDisplayName()) return ""
        return PlainTextComponentSerializer.plainText().serialize(meta.displayName()!!)
    }

    /** Applies a single-line lore while naming an item. */
    @JvmStatic
    fun nameItem(item: ItemStack, name: Component, lore: Component): ItemStack {
        var lore = lore
        lore = lore.decoration(TextDecoration.ITALIC, false)
        return nameItem(item, name, mutableListOf(lore))
    }

    /** Replaces lore on an item stack and removes italic styling from each line. */
    fun setLore(item: ItemStack, lore: MutableList<Component?>): ItemStack {
        item.editMeta(ItemMeta::class.java) { meta ->
            meta.lore(lore.map { it!!.decoration(TextDecoration.ITALIC, false) })
        }
        return item
    }

    /** Sets display name and optional lore on an item stack. */
    @JvmStatic
    @JvmOverloads
    fun nameItem(
        item: ItemStack,
        name: Component,
        lore: MutableList<Component?>? = null as MutableList<Component?>?,
    ): ItemStack {
        var name = name
        val meta =
            item.itemMeta
                ?: // Cannot name item without meta (probably air)
                return item

        name = name.decoration(TextDecoration.ITALIC, false)
        meta.displayName(name)

        if (lore != null) {
            meta.lore(lore.map { it!!.decoration(TextDecoration.ITALIC, false) })
        }

        item.itemMeta = meta
        return item
    }

    /** Compares enchantment sets for deterministic ordering. */
    fun compareEnchantments(itemA: ItemStack, itemB: ItemStack): Int {
        var aE = itemA.enchantments
        var bE = itemB.enchantments

        val aMeta = itemA.itemMeta
        if (aMeta is EnchantmentStorageMeta) {
            val stored = aMeta.storedEnchants
            if (stored.isNotEmpty()) {
                aE = stored
            }
        }

        val bMeta = itemB.itemMeta
        if (bMeta is EnchantmentStorageMeta) {
            val stored = bMeta.storedEnchants
            if (stored.isNotEmpty()) {
                bE = stored
            }
        }

        // Unenchanted first
        val aCount = aE.size
        val bCount = bE.size
        if (aCount == 0 && bCount == 0) {
            return 0
        } else if (aCount == 0) {
            return -1
        } else if (bCount == 0) {
            return 1
        }

        // More enchantments before fewer enchantments
        if (aCount != bCount) {
            return bCount - aCount
        }

        val aSorted = aE.entries.sortedWith(compareBy({ it.key!!.key.toString() }, { it.value }))
        val bSorted = bE.entries.sortedWith(compareBy({ it.key!!.key.toString() }, { it.value }))

        // Lastly, compare names and levels
        val aIt = aSorted.iterator()
        val bIt = bSorted.iterator()

        while (aIt.hasNext()) {
            val aEl = aIt.next()
            val bEl = bIt.next()

            // Lexicographic name comparison
            val nameDiff = aEl.key!!.key.toString().compareTo(bEl.key!!.key.toString())
            if (nameDiff != 0) {
                return nameDiff
            }

            // Level
            val levelDiff = bEl.value!! - aEl.value!!
            if (levelDiff != 0) {
                return levelDiff
            }
        }

        return 0
    }

    /**
     * Creates a player-head stack for an offline player, optionally skipping menu-head rendering.
     */
    @JvmStatic
    fun skullForPlayer(player: OfflinePlayer?, isForMenu: Boolean): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        if (!isForMenu || Core.instance()?.configPlayerHeadsInMenus == true) {
            item.editMeta(SkullMeta::class.java) { meta -> meta.owningPlayer = player }
        }
        return item
    }

    /** Creates a player head item from a base64 texture payload. */
    fun skullWithTexture(name: String, base64Texture: String): ItemStack {
        val profile = Bukkit.createProfileExact(SKULL_OWNER, "-")
        profile.setProperty(ProfileProperty("textures", base64Texture))

        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta
        val nameComponent =
            Component.text(name)
                .decoration(TextDecoration.ITALIC, false)
                .color(NamedTextColor.YELLOW)
        meta.displayName(nameComponent)
        meta.playerProfile = profile
        item.itemMeta = meta
        return item
    }

    /** Returns whether the given component is guarded by a hover sentinel. */
    @JvmStatic
    fun hasSentinel(component: Component?, sentinel: NamespacedKey): Boolean {
        if (component == null) {
            return false
        }

        val hover = component.hoverEvent() ?: return false

        val hoverText = hover.value()
        return hoverText is TextComponent &&
            hover.action() == HoverEvent.Action.SHOW_TEXT &&
            sentinel.toString() == hoverText.content()
    }

    /** Adds a hover sentinel marker to a component. */
    @JvmStatic
    fun addSentinel(component: Component, sentinel: NamespacedKey): Component {
        return component.hoverEvent(HoverEvent.showText(Component.text(sentinel.toString())))
    }

    /** Applies enchantments to an item from a compact definition string. */
    private fun applyEnchants(itemStack: ItemStack, enchants: String?): ItemStack {
        var enchants = enchants ?: return itemStack

        enchants = enchants.trim { it <= ' ' }
        require(!(!enchants.startsWith("{") || !enchants.endsWith("}"))) {
            "enchantments must be of form {<namespace:enchant>[*<level>][,<namespace:enchant>[*<level>]]...}"
        }

        val parts =
            enchants.substring(1, enchants.length - 1).split(',').dropLastWhile { it.isEmpty() }
        for (rawPart in parts) {
            val part: String = rawPart.trim { it <= ' ' }

            var key = part
            var level = 1
            val levelDelim = key.indexOf('*')
            if (levelDelim != -1) {
                level = key.substring(levelDelim + 1).toInt()
                key = key.substring(0, levelDelim)
            }

            val enchKey =
                NamespacedKey.fromString(key)
                    ?: throw IllegalArgumentException(
                        "Invalid enchantment key '$key' for item '$itemStack'"
                    )
            val ench: Enchantment? =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)[enchKey]
            requireNotNull(ench) { "Cannot apply unknown enchantment '$key' to item '$itemStack'" }

            if (itemStack.type == Material.ENCHANTED_BOOK) {
                val flevel = level
                itemStack.editMeta(EnchantmentStorageMeta::class.java) { meta ->
                    meta.addStoredEnchant(
                        ench,
                        flevel,
                        false,
                    )
                }
            } else {
                itemStack.addEnchantment(ench, level)
            }
        }

        if (parts.isNotEmpty()) {
            Core.instance()?.enchantmentManager?.updateEnchantedItem(itemStack)
        }
        return itemStack
    }

    /** Parses an item definition string into an [ItemStack] and simple-material flag. */
    fun itemstackFromString(definition: String): Pair<ItemStack?, Boolean?> {
        // NOTE: Override to allow seamless migration from pre 1.21.9 to 1.21.9+
        var definition = definition
        if ("minecraft:chain".equals(definition, ignoreCase = true)) {
            definition = "minecraft:iron_chain"
        }

        // namespace:key[[components]][#enchants{}], where the key can reference a
        // material, head material or customitem.
        val enchantsDelim = definition.indexOf("#enchants{")
        var enchants: String? = null
        if (enchantsDelim != -1) {
            enchants = definition.substring(enchantsDelim + 9) // Let it start at '{'
            definition = definition.substring(0, enchantsDelim)
        }

        val nbtDelim = definition.indexOf('[')
        val key: NamespacedKey? =
            if (nbtDelim == -1) {
                NamespacedKey.fromString(definition)
            } else {
                NamespacedKey.fromString(definition.substring(0, nbtDelim))
            }

        val emat = ExtendedMaterial.from(key!!)
        requireNotNull(emat) { "Invalid extended material definition: $definition" }

        // First, create the itemstack as if we had no NBT information.
        val itemStack = emat.item()

        // If there is no NBT information, we can return here.
        if (nbtDelim == -1) {
            return Pair(
                applyEnchants(itemStack!!, enchants),
                emat.isSimpleMaterial && enchants == null,
            )
        }

        // Parse the NBT by using minecraft's internal parser with the base material
        // of whatever the extended material gave us.
        val vanillaDefinition = itemStack!!.type.key().toString() + definition.substring(nbtDelim)
        try {
            val parsedNbt =
                ItemParser(Commands.createValidationContext(VanillaRegistries.createWorldLookup()))
                    .parse(StringReader(vanillaDefinition))
                    .components()

            // Now apply the NBT be parsed by minecraft's internal parser to the itemstack.
            val nmsItem = itemHandle(itemStack)!!.copy()
            nmsItem.applyComponents(parsedNbt)

            return Pair(applyEnchants(CraftItemStack.asBukkitMirror(nmsItem), enchants), false)
        } catch (e: CommandSyntaxException) {
            throw IllegalArgumentException("Could not parse NBT of item definition: $definition", e)
        }
    }

    /** Comparator for stable ordering of item stacks in selectors. */
    class ItemStackComparator : Comparator<ItemStack?> {
        /** Compares two item stacks by creative tab, id, damage, count, and enchantments. */
        override fun compare(a: ItemStack?, b: ItemStack?): Int {
            if (a == null && b == null) {
                return 0
            } else if (a == null) {
                return 1
            } else if (b == null) {
                return -1
            }

            val nA = itemHandle(a)
            val nB = itemHandle(b)
            if (nA!!.isEmpty) {
                return if (nB!!.isEmpty) 0 else 1
            } else if (nB!!.isEmpty) {
                return -1
            }

            // By creative mode tab
            val creativeModeTabDiff = creativeTabId(nA) - creativeTabId(nB)
            if (creativeModeTabDiff != 0) {
                return creativeModeTabDiff
            }

            // By id
            val idDiff = Item.getId(nA.item) - Item.getId(nB.item)
            if (idDiff != 0) {
                return idDiff
            }

            // By damage
            val damageDiff = nA.damageValue - nB.damageValue
            if (damageDiff != 0) {
                return damageDiff
            }

            // By count
            val countDiff = nB.count - nA.count
            if (countDiff != 0) {
                return countDiff
            }

            // By enchantments
            return compareEnchantments(a, b)
        }
    }
}
