package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class HarvestListener extends Listener<Trifles> {

    public HarvestListener(Context<Trifles> context) {
        super(
            context.group(
                "better_harvesting",
                "Enables better harvesting. Right clicking on grown crops with bare hands will then harvest the plant and also replant it."
            )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_harvest(PlayerInteractEvent event) {
        if (
            !event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
        ) {
            return;
        }

        // Only harvest when right-clicking some plant type
        final var type = event.getClickedBlock().getType();
        if (!is_seeded_plant(type)) {
            return;
        }

        final var player = event.getPlayer();
        if (harvest_plant(player, event.getClickedBlock())) {
            swing_arm(player, event.getHand());
        }
    }
}
