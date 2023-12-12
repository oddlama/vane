package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessageArray;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.lang.TranslatedMessageArray;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.Nms;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.advancements.CriteriaTriggers;

@VaneItem(name = "trowel", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 800, model_data = 0x76000e, version = 1)
public class Trowel extends CustomItem<Trifles> {
	private static final NamespacedKey SENTINEL = StorageUtil.namespaced_key("vane", "trowel_lore");
	public static final NamespacedKey FEED_SOURCE = StorageUtil.namespaced_key("vane", "feed_source");
	private static Random random = new Random(23584982345l);

	public enum FeedSource {
		HOTBAR("Hotbar", new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }),
		FIRST_ROW("First Inventory Row", new int[] { 9, 10, 11, 12, 13, 14, 15, 16, 17 }),
		SECOND_ROW("Second Inventory Row", new int[] { 18, 19, 20, 21, 22, 23, 24, 25, 26 }),
		THIRD_ROW("Third Inventory Row", new int[] { 27, 28, 29, 30, 31, 32, 33, 34, 35 });

		private String display_name;
		private int[] slots;

		private FeedSource(final String display_name, int[] slots) {
			this.display_name = display_name;
			this.slots = slots;
		}

		public String display_name() {
			return display_name;
		}

		public FeedSource next() {
			return FeedSource.values()[(this.ordinal() + 1) % FeedSource.values().length];
		}

		public int[] slots() {
			return slots;
		}
	}

	@LangMessageArray
	public TranslatedMessageArray lang_lore;

	public Trowel(final Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
				.shape("  s", "mm ")
				.set_ingredient('m', Material.IRON_INGOT)
				.set_ingredient('s', Material.STICK)
				.result(key().toString()));
	}

	/**
	 * Returns true if the given component is associated to the trowel.
	 */
	private static boolean is_trowel_lore(final Component component) {
		return ItemUtil.has_sentinel(component, SENTINEL);
	}

	private FeedSource feed_source(final ItemStack item_stack) {
		if (!item_stack.hasItemMeta()) {
			return FeedSource.HOTBAR;
		}
		final var ord = item_stack.getItemMeta().getPersistentDataContainer().getOrDefault(FEED_SOURCE,
				PersistentDataType.INTEGER, 0);
		if (ord < 0 || ord > FeedSource.values().length) {
			return FeedSource.HOTBAR;
		}
		return FeedSource.values()[ord];
	}

	private void feed_source(final ItemStack item_stack, final FeedSource feed_source) {
		item_stack.editMeta(meta -> meta.getPersistentDataContainer().set(FEED_SOURCE, PersistentDataType.INTEGER, feed_source.ordinal()));
	}

	private void update_lore(final ItemStack item_stack) {
		var lore = item_stack.lore();
		if (lore == null) {
			lore = new ArrayList<Component>();
		}

		// Remove old lore, add updated lore
		lore.removeIf(Trowel::is_trowel_lore);

		final var feed_source = feed_source(item_stack);
		lore.addAll(lang_lore
				.format("§a" + feed_source)
				.stream()
				.map(x -> ItemUtil.add_sentinel(x, SENTINEL)).toList());

		item_stack.lore(lore);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_player_click_inventory(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		// Only on right-click item, when nothing is on the cursor
		if (event.getAction() != InventoryAction.PICKUP_HALF || (event.getCursor() != null && event.getCursor().getType() != Material.AIR)) {
			return;
		}

		final var item = event.getCurrentItem();
		final var custom_item = get_module().core.item_registry().get(item);
		if (!(custom_item instanceof Trowel trowel) || !trowel.enabled()) {
			return;
		}

		// Use next feed source
		final var feed_source = feed_source(item);
		feed_source(item, feed_source.next());
		update_lore(item);

		player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f);
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact_block(final PlayerInteractEvent event) {
		// Skip if no block was right-clicked or hand isn't main hand
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		// With a trowel in main hand
		final var player = event.getPlayer();
		final var item_in_hand = player.getEquipment().getItem(EquipmentSlot.HAND);
		final var custom_item = get_module().core.item_registry().get(item_in_hand);
		if (!(custom_item instanceof Trowel trowel) || !trowel.enabled()) {
			return;
		}

		// Prevent offhand from triggering (e.g. placing torches)
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);

		// Select a random block from the feed source and place it
		final var block = event.getClickedBlock();
		final var inventory = player.getInventory();
		final var feed_source = feed_source(item_in_hand);
		final var possible_slots = feed_source.slots().clone();
		int count = possible_slots.length;
		while (count > 0) {
			final var index = random.nextInt(count);
			final var item_stack = inventory.getItem(possible_slots[index]);
			// Skip empty slots and items that are not placeable blocks
			if (item_stack == null || !item_stack.getType().isBlock() || Tag.SHULKER_BOXES.isTagged(item_stack.getType())) {
				// Eliminate end of list, so copy item at end of list to the index (< count).
				possible_slots[index] = possible_slots[--count];
				continue;
			}

			final var nms_item = Nms.item_handle(item_stack);
			final var nms_player = Nms.player_handle(player);
			final var nms_world = Nms.world_handle(player.getWorld());

			// Prepare context to place the item via NMS
			final var direction = CraftBlock.blockFaceToNotch(event.getBlockFace());
			final var block_pos = new BlockPos(block.getX(), block.getY(), block.getZ());
			final var interaction_point = event.getInteractionPoint();
			final var hit_pos = new Vec3(interaction_point.getX(), interaction_point.getY(), interaction_point.getZ());
			final var block_hit_result = new BlockHitResult(hit_pos, direction, block_pos, false);
			final var amount_pre = nms_item.getCount();
			final var action_context = new UseOnContext(nms_world, nms_player, InteractionHand.MAIN_HAND, nms_item, block_hit_result);

			// Get sound now, otherwise the itemstack might be consumed afterwards
			SoundType sound_type = null;
			if (nms_item.getItem() instanceof BlockItem block_item) {
				final var place_state = block_item.getBlock().getStateForPlacement(new BlockPlaceContext(action_context));
				sound_type = place_state.getSoundType();
			}

			// Place the item by calling NMS to get correct placing behavior
			final var result = nms_item.useOn(action_context);

			// Don't consume item in creative mode
			if (player.getGameMode() == GameMode.CREATIVE) {
				nms_item.setCount(amount_pre);
			}

			if (result.consumesAction()) {
				swing_arm(player, EquipmentSlot.HAND);
				damage_item(player, item_in_hand, 1);
				if (sound_type != null) {
					nms_world.playSound(null, block_pos, sound_type.getPlaceSound(), SoundSource.BLOCKS, (sound_type.getVolume() + 1.0F) / 2.0F, sound_type.getPitch() * 0.8F);
				}
				CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(nms_player, block_pos, nms_item);
			}

			nms_player.connection.send(new ClientboundBlockUpdatePacket(nms_world, block_pos));
			nms_player.connection.send(new ClientboundBlockUpdatePacket(nms_world, block_pos.relative(direction)));
			return;
		}

		// No item found in any possible slot.
		player.playSound(player, Sound.UI_STONECUTTER_SELECT_RECIPE, SoundCategory.MASTER, 1.0f, 2.0f);
	}

	@Override
	public ItemStack updateItemStack(final ItemStack item_stack) {
		update_lore(item_stack);
		return item_stack;
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
	}
}
