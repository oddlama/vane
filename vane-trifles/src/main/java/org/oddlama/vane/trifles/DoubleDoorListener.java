package org.oddlama.vane.trifles;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class DoubleDoorListener extends Listener<Trifles> {

    public DoubleDoorListener(Context<Trifles> context) {
        super(
            context.group(
                "doubledoor",
                "Enable updating of double doors automatically when one of the doors is changed."
            )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_interact(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handle_double_door(event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_entity_interact(EntityInteractEvent event) {
        final var block = event.getBlock();
        handle_double_door(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_block_redstone(BlockRedstoneEvent event) {
        var now = event.getNewCurrent();
        var old = event.getOldCurrent();
        if (now != old && (now == 0 || old == 0)) {
            // only on / off changes
            handle_double_door(event.getBlock());
        }
    }

    public void handle_double_door(final Block block) {
        final var first = SingleDoor.create_door_from_block(block);
        if (first == null) {
            return;
        }
        final var second = first.get_second_door();
        if (second == null) {
            return;
        }

        // Update second door state directly after the event (delay 0)
        schedule_next_tick(() -> {
            // Make sure to include changes from last tick
            if (!first.update_cached_state() || !second.update_cached_state()) {
                return;
            }

            second.set_open(first.isOpen());
        });
    }
}
