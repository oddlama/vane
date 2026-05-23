package org.oddlama.vane.util;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Nms {

	public static ServerPlayer get_player(Player player) {
		return ((CraftPlayer)player).getHandle();
	}

	public static Entity entity_handle(final org.bukkit.entity.Entity entity) {
		return ((CraftEntity)entity).getHandle();
	}

	@NotNull
	public static org.bukkit.inventory.ItemStack bukkit_item_stack(ItemStack stack) {
		return CraftItemStack.asCraftMirror(stack);
	}


	public static ItemStack item_handle(org.bukkit.inventory.ItemStack item_stack) {
		if (item_stack == null) {
			return null;
		}

		if (!(item_stack instanceof CraftItemStack)) {
			return CraftItemStack.asNMSCopy(item_stack);
		}

		try {
			final var handle = CraftItemStack.class.getDeclaredField("handle");
			handle.setAccessible(true);
			return (ItemStack)handle.get(item_stack);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	public static ServerPlayer player_handle(org.bukkit.entity.Player player) {
		if (!(player instanceof CraftPlayer)) {
			return null;
		}
		return ((CraftPlayer)player).getHandle();
	}

	public static ServerLevel world_handle(org.bukkit.World world) {
		return ((CraftWorld)world).getHandle();
	}

	public static DedicatedServer server_handle() {
		final var bukkit_server = Bukkit.getServer();
		return ((CraftServer)bukkit_server).getServer();
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	public static void register_entity(
	    final NamespacedKey base_entity_type,
	    final String pseudo_namespace,
	    final String key,
        final EntityType.Builder<?> builder
    ) {
		final var id = pseudo_namespace + "_" + key;
		// From:
		// https://papermc.io/forums/t/register-and-spawn-a-custom-entity-on-1-13-x/293,
		// adapted for 1.18
		// Get the datafixer
		final var world_version = SharedConstants.getCurrentVersion().dataVersion().version();
		final var world_version_key = DataFixUtils.makeKey(world_version);
		final var data_types = DataFixers.getDataFixer()
		                           .getSchema(world_version_key)
		                           .findChoiceType(References.ENTITY)
		                           .types();
		final var data_types_map = (Map<Object, Type<?>>)data_types;
		// Inject the new custom entity (this registers the key/id with the server,
		// so it will be available in vanilla constructs like the /summon command)
		data_types_map.put("minecraft:" + id, data_types_map.get(base_entity_type.toString()));
		// Store a new type in registry
		final var rk = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(id));
		Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(rk));
	}

	public static void spawn(org.bukkit.World world, Entity entity) {
		world_handle(world).addFreshEntity(entity);
	}

	public static int unlock_all_recipes(final org.bukkit.entity.Player player) {
		final var recipes = server_handle().getRecipeManager().getRecipes();
		return player_handle(player).awardRecipes(recipes);
	}

	public static int creative_tab_id(final ItemStack item_stack) {
		// TODO FIXME BUG this is broken and always returns 0
		return (int)CreativeModeTabs.allTabs().stream().takeWhile(tab -> tab.contains(item_stack)).count();
	}

	public static void set_air_no_drops(final org.bukkit.block.Block block) {
        final var entity = world_handle(block.getWorld()).getBlockEntity(
            new BlockPos(block.getX(), block.getY(), block.getZ())
        );
		block.setType(Material.AIR, false);
	}
}
