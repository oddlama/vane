package org.oddlama.vane.core.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.loot.Lootable;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class LootChestProtector extends Listener<Core> {

    // Prevent loot chest destruction
    private final Map<Block, Map<UUID, Long>> loot_break_attempts = new HashMap<>();

    // TODO(legacy): this should become a separate group instead of having
    // this boolean.
    @ConfigBoolean(
        def = true,
        desc = "Prevent players from breaking blocks with loot-tables (like treasure chests) when they first attempt to destroy it. They still can break it, but must do so within a short timeframe."
    )
    public boolean config_warn_breaking_loot_blocks;

    @LangMessage
    public TranslatedMessage lang_break_loot_block_prevented;

    public LootChestProtector(Context<Core> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on_break_loot_chest(final BlockBreakEvent event) {
        if (!config_warn_breaking_loot_blocks) {
            return;
        }

        final var state = event.getBlock().getState(false);
        if (!(state instanceof Lootable)) {
            return;
        }

        final var lootable = (Lootable) state;
        if (!lootable.hasLootTable()) {
            return;
        }

        final var block = event.getBlock();
        final var player = event.getPlayer();
        var block_attempts = loot_break_attempts.get(block);
        final var now = System.currentTimeMillis();
        if (block_attempts != null) {
            final var player_attempt_time = block_attempts.get(player.getUniqueId());
            if (player_attempt_time != null) {
                final var elapsed = now - player_attempt_time;
                if (elapsed > 5000 && elapsed < 30000) {
                    // Allow
                    return;
                }
            } else {
                block_attempts.put(player.getUniqueId(), now);
            }
        } else {
            block_attempts = new HashMap<UUID, Long>();
            block_attempts.put(player.getUniqueId(), now);
            loot_break_attempts.put(block, block_attempts);
        }

        lang_break_loot_block_prevented.send(player);
        event.setCancelled(true);
    }
}
