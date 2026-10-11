package org.oddlama.vane.util

import com.mojang.datafixers.DataFixUtils
import com.mojang.datafixers.types.Type
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.level.block.Blocks
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import net.minecraft.world.level.block.Block as NmsBlock

/**
 * Bridges Bukkit/Paper APIs to internal NMS handles.
 */
object Nms {
    /** Returns the NMS handle for a player. */
    @JvmStatic
    fun getPlayer(player: Player): ServerPlayer = (player as CraftPlayer).handle

    /** Returns the NMS handle for a Bukkit entity. */
    @JvmStatic
    fun entityHandle(entity: org.bukkit.entity.Entity): Entity = (entity as CraftEntity).handle

    /** Returns or creates the NMS handle for a Bukkit item stack. */
    @JvmStatic
    fun itemHandle(itemStack: ItemStack?): net.minecraft.world.item.ItemStack? = when (itemStack) {
        null -> null
        is CraftItemStack -> itemStack.handle
        else -> CraftItemStack.asNMSCopy(itemStack)
    }

    /** Returns the NMS handle for a player, or null when incompatible. */
    @JvmStatic
    fun playerHandle(player: Player?): ServerPlayer? = (player as? CraftPlayer)?.handle

    /** Returns the NMS handle for a world. */
    @JvmStatic
    fun worldHandle(world: World): ServerLevel = (world as CraftWorld).handle

    /** Returns the dedicated server handle. */
    @JvmStatic
    fun serverHandle(): DedicatedServer = (Bukkit.getServer() as CraftServer).server

    /**
     * Registers a custom entity type by cloning datafixer mappings from a base entity.
     */
    @JvmStatic
    fun registerEntity(
        baseEntityType: NamespacedKey,
        pseudoNamespace: String?,
        key: String?,
        builder: EntityType.Builder<*>
    ) {
        val id = "${pseudoNamespace}_$key"
        val worldVersion = SharedConstants.getCurrentVersion().dataVersion().version()
        val worldVersionKey = DataFixUtils.makeKey(worldVersion)

        @Suppress("UNCHECKED_CAST")
        val dataTypesMap = DataFixers.getDataFixer()
            .getSchema(worldVersionKey)
            .findChoiceType(References.ENTITY)
            .types() as MutableMap<Any, Type<*>?>
        dataTypesMap["minecraft:$id"] = dataTypesMap[baseEntityType.toString()]
        val rk: ResourceKey<EntityType<*>> =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(id))
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(rk))
    }

    /** Spawns an NMS entity into a Bukkit world. */
    @JvmStatic
    fun spawn(world: World, entity: Entity) = worldHandle(world).addFreshEntity(entity)

    /** Unlocks all known recipes for a player. */
    @JvmStatic
    fun unlockAllRecipes(player: Player): Int {
        val recipes: MutableCollection<RecipeHolder<*>> = serverHandle().recipeManager.getRecipes()
        return getPlayer(player).awardRecipes(recipes)
    }

    /**
     * Returns a sortable creative-tab index for an NMS item stack, or the number of category tabs
     * when the item is in none of them. Non-category tabs (search, hotbar, inventory) are skipped
     * because the search tab contains every item.
     *
     * Only the client normally builds creative tab contents, so they are built here on first use.
     * Matching uses the item's default instance because tabs only contain exact component variants.
     */
    @JvmStatic
    fun creativeTabId(itemStack: net.minecraft.world.item.ItemStack): Int {
        val server = serverHandle()
        CreativeModeTabs.tryRebuildTabContents(server.worldData.enabledFeatures(), true, server.registryAccess())
        val defaultInstance = itemStack.item.defaultInstance
        return CreativeModeTabs.allTabs()
            .filter { it.type == CreativeModeTab.Type.CATEGORY }
            .takeWhile { !it.contains(defaultInstance) }
            .count()
    }

    /**
     * Sets a block to air without dropping loot. Block entity side effects are skipped too,
     * otherwise containers (chests, barrels, ...) spill their contents when removed.
     */
    @JvmStatic
    fun setAirNoDrops(block: Block) {
        worldHandle(block.world).setBlock(
            BlockPos(block.x, block.y, block.z),
            Blocks.AIR.defaultBlockState(),
            NmsBlock.UPDATE_CLIENTS or NmsBlock.UPDATE_SKIP_ALL_SIDEEFFECTS
        )
    }
}
