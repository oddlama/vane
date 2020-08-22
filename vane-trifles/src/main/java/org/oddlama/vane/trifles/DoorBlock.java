package org.oddlama.vane.trifles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.oddlama.vane.util.BlockUtil;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class DoorBlock {
	private Block lower_block; // Must be lower block
	private Door lower;
	private Door upper;

	public boolean update_state() {
		// Update to the the current state if possible
		final var lower_data = lower_block.getBlockData();
		if (lower_data instanceof Door) {
			lower = (Door)lower_data;
		} else {
			return false;
		}

		final var upper_data = lower_block.getRelative(BlockFace.UP).getBlockData();
		if (upper_data instanceof Door) {
			upper = (Door)upper_data;
		} else {
			return false;
		}

		return true;
	}

	public BlockFace get_facing() { return lower.getFacing(); }
	public Door.Hinge get_hinge() { return upper.getHinge(); }
	public boolean is_open() { return lower.isOpen(); }
	public void set_open(boolean open) {
		var data = (Door)lower_block.getBlockData();
		data.setOpen(open);
		lower_block.setBlockData(data);
	}

	public static DoorBlock get_door(final Block block) {
		final var block_data = block.getBlockData();
		if (!(block_data instanceof Door)) {
			return null;
		}

		final var door = (Door)block_data;
		final var door_block = new DoorBlock();

		if (door.getHalf() == Bisected.Half.TOP) {
			final var tmp = block.getRelative(BlockFace.DOWN);
			final var data = tmp.getBlockData();

			if (data instanceof Door) {
				door_block.lower_block = tmp;
				door_block.lower = (Door)data;
				door_block.upper = door;
			} else {
				return null;
			}
		} else {
			final var data = block.getRelative(BlockFace.UP).getBlockData();
			if (data instanceof Door) {
				door_block.lower_block = block;
				door_block.lower = door;
				door_block.upper = (Door)data;
			} else {
				return null;
			}
		}

		return door_block;
	}

	public static DoorBlock get_second_door(final DoorBlock first) {
		if (first == null) {
			return null;
		}

		for (var face : BlockUtil.XZ_FACES) {
			final var block = first.lower_block.getRelative(face);
			if (block == null) {
				continue;
			}

			final var second = DoorBlock.get_door(block);
			if (second == null) {
				continue;
			}

			// Not on same height
			if (!BlockUtil.equals_pos(first.lower_block, second.lower_block.getRelative(face.getOppositeFace()))) {
				continue;
			}

			// Not same face or same hinge
			if (first.get_facing() != second.get_facing() || first.get_hinge() == second.get_hinge()) {
				continue;
			}

			return second;
		}

		return null;
	}
}
