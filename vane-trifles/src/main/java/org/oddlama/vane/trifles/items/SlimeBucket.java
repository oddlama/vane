package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.give_items;

import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

import net.kyori.adventure.key.Key;

@VaneItem(name = "slime_bucket", base = Material.SLIME_BALL, model_data = 0x760014 /* and 0x760015 */, version = 1)
public class SlimeBucket extends CustomItem<Trifles> {
	private static final int CUSTOM_MODEL_DATA_QUIET = 0x760014;
	private static final int CUSTOM_MODEL_DATA_JUMPY = 0x760015;
	private HashSet<UUID> players_in_slime_chunks = new HashSet<>();

	public SlimeBucket(Context<Trifles> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact_entity(final PlayerInteractEntityEvent event) {
		final var entity = event.getRightClicked();
		// Only when a tiny slime is right-clicked
		if (!(entity instanceof Slime slime) || slime.getSize() != 1) {
			return;
		}

		if (entity.isDead()) {
			return;
		}

		// With an empty bucket in the main hand
		final var player = event.getPlayer();
		final var item_in_hand = player.getEquipment().getItem(event.getHand());
		if (item_in_hand.getType() != Material.BUCKET) {
			return;
		}

		// Consume one bucket to create a slime bucket.
		entity.remove();
		swing_arm(player, event.getHand());
		player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f);

		// Create slime bucket with correct custom model data
		final var new_stack = newStack();
		new_stack.editMeta(meta -> {
			final var correct_model_data = player.getChunk().isSlimeChunk() ? CUSTOM_MODEL_DATA_JUMPY : CUSTOM_MODEL_DATA_QUIET;
			meta.setCustomModelData(correct_model_data);
		});

		if (item_in_hand.getAmount() == 1) {
			// Replace with Slime Bucket
			player.getEquipment().setItem(event.getHand(), new_stack);
		} else {
			// Reduce the amount and add SlimeBucket to inventory
			item_in_hand.setAmount(item_in_hand.getAmount() - 1);
			give_items(player, new_stack, 1);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact(final PlayerInteractEvent event) {
		// Skip if no block was right-clicked
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// With a slime_bucket in main hand
		final var player = event.getPlayer();
		final var item_in_hand = player.getEquipment().getItem(event.getHand());
		final var custom_item = get_module().core.item_registry().get(item_in_hand);
		if (!(custom_item instanceof SlimeBucket slime_bucket) || !slime_bucket.enabled()) {
			return;
		}

		// Prevent offhand from triggering (e.g., placing torches)
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);

		// Place slime back into the world
		final var loc = event.getInteractionPoint();
		loc.getWorld().spawnEntity(loc, EntityType.SLIME, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
			if (entity instanceof Slime slime) {
				slime.setSize(1);
			}
		});

		player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f);
		swing_arm(player, event.getHand());
		if (item_in_hand.getAmount() == 1) {
			// Replace with empty bucket
			player.getEquipment().setItem(event.getHand(), new ItemStack(Material.BUCKET));
		} else {
			// Reduce the amount and add empty bucket to inventory
			item_in_hand.setAmount(item_in_hand.getAmount() - 1);
			give_items(player, new ItemStack(Material.BUCKET), 1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		final var player = event.getPlayer();
		final var in_slime_chunk = event.getTo().getChunk().isSlimeChunk();
		final var in_set = players_in_slime_chunks.contains(player.getUniqueId());

		if (in_set != in_slime_chunk) {
			if (in_slime_chunk) {
				players_in_slime_chunks.add(player.getUniqueId());
			} else {
				players_in_slime_chunks.remove(player.getUniqueId());
			}

			final var correct_model_data = in_slime_chunk ? CUSTOM_MODEL_DATA_JUMPY : CUSTOM_MODEL_DATA_QUIET;
			for (final var item : player.getInventory().getContents()) {
				final var custom_item = get_module().core.item_registry().get(item);
				if (custom_item instanceof SlimeBucket slime_bucket && slime_bucket.enabled()) {
					// Update slime bucket custom model data
					item.editMeta(meta -> {
						meta.setCustomModelData(correct_model_data);
					});
				}
			}
		}
	}

	@Override
	public void addResources(final ResourcePackGenerator rp) throws IOException {
		{ // Base
			final var resource_name = "items/" + key().value() + ".png";
			final var resource = get_module().getResource(resource_name);
			if (resource == null) {
				throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
			}
			rp.add_item_model(key(), resource, Key.key(Key.MINECRAFT_NAMESPACE, "item/generated"));
			rp.add_item_override(baseMaterial().getKey(), key(), predicate -> {
				predicate.put("custom_model_data", CUSTOM_MODEL_DATA_QUIET);
			});
		}

		{ // Excited
			final var excited_key = StorageUtil.namespaced_key(key().namespace(), key().value() + "_excited");
			final var resource_name = "items/" + excited_key.value() + ".png";
			final var resource = get_module().getResource(resource_name);
			if (resource == null) {
				throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
			}
			final var resource_mcmeta = get_module().getResource(resource_name + ".mcmeta");
			if (resource_mcmeta == null) {
				throw new RuntimeException("Missing resource '" + resource_name + ".mcmeta'. This is a bug.");
			}
			rp.add_item_model(excited_key, resource, resource_mcmeta, Key.key(Key.MINECRAFT_NAMESPACE, "item/generated"));
			rp.add_item_override(baseMaterial().getKey(), excited_key, predicate -> {
				predicate.put("custom_model_data", CUSTOM_MODEL_DATA_JUMPY);
			});
		}
	}
}
