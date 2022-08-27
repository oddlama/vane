package org.oddlama.vane.util;

import java.util.Map;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftNamespacedKey;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class Nms {

	public static ServerPlayer get_player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	public static Entity entity_handle(final org.bukkit.entity.Entity entity) {
		return ((CraftEntity) entity).getHandle();
	}

	public static void register_enchantment(NamespacedKey key, Enchantment enchantment) {
		Registry.register(Registry.ENCHANTMENT, new ResourceLocation(key.getNamespace(), key.getKey()), enchantment);
	}

	public static org.bukkit.enchantments.Enchantment bukkit_enchantment(Enchantment enchantment) {
		final var key = Registry.ENCHANTMENT.getKey(enchantment);
		return org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(key));
	}

	@NotNull
	public static org.bukkit.inventory.ItemStack bukkit_item_stack(ItemStack stack) {
		return CraftItemStack.asCraftMirror(stack);
	}

	public static EnchantmentCategory enchantment_slot_type(EnchantmentTarget target) {
		switch (target) {
			case ARMOR:
				return EnchantmentCategory.ARMOR;
			case ARMOR_FEET:
				return EnchantmentCategory.ARMOR_FEET;
			case ARMOR_HEAD:
				return EnchantmentCategory.ARMOR_HEAD;
			case ARMOR_LEGS:
				return EnchantmentCategory.ARMOR_LEGS;
			case ARMOR_TORSO:
				return EnchantmentCategory.ARMOR_CHEST;
			case TOOL:
				return EnchantmentCategory.DIGGER;
			case WEAPON:
				return EnchantmentCategory.WEAPON;
			case BOW:
				return EnchantmentCategory.BOW;
			case FISHING_ROD:
				return EnchantmentCategory.FISHING_ROD;
			case BREAKABLE:
				return EnchantmentCategory.BREAKABLE;
			case WEARABLE:
				return EnchantmentCategory.WEARABLE;
			case TRIDENT:
				return EnchantmentCategory.TRIDENT;
			case CROSSBOW:
				return EnchantmentCategory.CROSSBOW;
			case VANISHABLE:
				return EnchantmentCategory.VANISHABLE;
			default:
				return null;
		}
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
			return (ItemStack) handle.get(item_stack);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	public static ServerPlayer player_handle(org.bukkit.entity.Player player) {
		if (!(player instanceof CraftPlayer)) {
			return null;
		}
		return ((CraftPlayer) player).getHandle();
	}

	public static ServerLevel world_handle(org.bukkit.World world) {
		return ((CraftWorld) world).getHandle();
	}

	public static DedicatedServer server_handle() {
		final var bukkit_server = Bukkit.getServer();
		return ((CraftServer) bukkit_server).getServer();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void register_entity(
			final NamespacedKey base_entity_type,
			final String pseudo_namespace,
			final String key,
			final EntityType.Builder<?> builder) {
		final var id = pseudo_namespace + "_" + key;
		// From:
		// https://papermc.io/forums/t/register-and-spawn-a-custom-entity-on-1-13-x/293,
		// adapted for 1.18
		// Get the datafixer
		final var world_version = SharedConstants.getCurrentVersion().getWorldVersion();
		final var world_version_key = DataFixUtils.makeKey(world_version);
		final var data_types = DataFixers
				.getDataFixer()
				.getSchema(world_version_key)
				.findChoiceType(References.ENTITY)
				.types();
		final var data_types_map = (Map<Object, Type<?>>) data_types;
		// Inject the new custom entity (this registers the key/id with the server,
		// so it will be available in vanilla constructs like the /summon command)
		data_types_map.put("minecraft:" + id, data_types_map.get(base_entity_type.toString()));
		// Store new type in registry
		Registry.register(Registry.ENTITY_TYPE, id, builder.build(id));
	}

	public static void spawn(org.bukkit.World world, Entity entity) {
		world_handle(world).addFreshEntity(entity);
	}

	public static int unlock_all_recipes(final org.bukkit.entity.Player player) {
		final var recipes = server_handle().getRecipeManager().getRecipes();
		return player_handle(player).awardRecipes(recipes);
	}

	public static int creative_tab_id(final Item item) {
		final var tab = item.getItemCategory();
		return tab == null ? Integer.MAX_VALUE : tab.getId();
	}

	public static void set_air_no_drops(final org.bukkit.block.Block block) {
		final var entity = world_handle(block.getWorld())
				.getBlockEntity(new BlockPos(block.getX(), block.getY(), block.getZ()));
		Clearable.tryClear(entity);
		block.setType(Material.AIR, false);
	}
}
