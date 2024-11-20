package org.oddlama.vane.portals;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.portal.PortalBlock;

public class PortalActivator extends Listener<Portals> {

    public PortalActivator(Context<Portals> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void on_player_interact_console(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        // Abort if the table is not a console
        final var block = event.getClickedBlock();
        final var portal_block = get_module().portal_block_for(block);
        if (portal_block == null || portal_block.type() != PortalBlock.Type.CONSOLE) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        final var player = event.getPlayer();
        final var portal = get_module().portal_for(portal_block);
        if (portal.open_console(get_module(), player, block)) {
            swing_arm(player, event.getHand());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void on_player_interact_switch(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        final var block = event.getClickedBlock();
        final boolean allow_disable;
        if (block.getType() == Material.LEVER) {
            allow_disable = true;
        } else if (Tag.BUTTONS.isTagged(block.getType())) {
            allow_disable = false;
        } else {
            return;
        }

        // Get base block the switch is attached to
        final var bswitch = (Switch) block.getBlockData();
        final BlockFace attached_face;
        switch (bswitch.getAttachedFace()) {
            default:
            case WALL:
                attached_face = bswitch.getFacing().getOppositeFace();
                break;
            case CEILING:
                attached_face = BlockFace.UP;
                break;
            case FLOOR:
                attached_face = BlockFace.DOWN;
                break;
        }

        // Find controlled portal
        final var base = block.getRelative(attached_face);
        final var portal = get_module().controlled_portal(base);
        if (portal == null) {
            return;
        }

        final var player = event.getPlayer();
        final var active = get_module().is_activated(portal);
        if (bswitch.isPowered() && allow_disable) {
            if (!active) {
                return;
            }

            // Switch is being switched off → deactivate
            if (!portal.deactivate(get_module(), player)) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        } else {
            if (active) {
                return;
            }

            // Switch is being switched on → activate
            if (!portal.activate(get_module(), player)) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_block_redstone(final BlockRedstoneEvent event) {
        // Only on rising edge.
        if (event.getOldCurrent() != 0 || event.getNewCurrent() == 0) {
            return;
        }

        // Only repeaters
        final var block = event.getBlock();
        if (block.getType() != Material.REPEATER) {
            return;
        }

        // Get the block it's pointing towards. (Opposite of block's facing for repeaters)
        final var repeater = (Repeater) block.getBlockData();
        final var into_block = block.getRelative(repeater.getFacing().getOppositeFace());

        // Find controlled portal
        final var portal = get_module().portal_for(into_block);
        if (portal == null) {
            return;
        }

        portal.activate(get_module(), null);
    }
}
