package org.oddlama.vane.core.misc;

import static org.oddlama.vane.util.BlockUtil.drop_naturally;
import static org.oddlama.vane.util.BlockUtil.texture_from_skull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.material.HeadMaterialLibrary;
import org.oddlama.vane.core.module.Context;

public class HeadLibrary extends Listener<Core> {

    @ConfigBoolean(def = true, desc = "When a player head is broken by a player that exists in /heads, drop the correctly named item as seen in /heads. You can disable this if it interferes with similarly textured heads from other plugins.")
    public boolean config_player_head_drops;

    public HeadLibrary(Context<Core> context) {
        super(context);
        // Load a head material library
        get_module().log.info("Loading head library...");
        try {
            String json = null;
            try (var input = get_module().getResource("head_library.json")) {
                if (input != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(input, StandardCharsets.UTF_8))) {
                        json = reader.lines().collect(Collectors.joining("\n"));
                    }
                }
            }
            if (json == null) {
                throw new IOException("Failed to get contents of resource head_library.json");
            }
            HeadMaterialLibrary.load(json);
        } catch (IOException e) {
            get_module().log.log(Level.SEVERE, "Error while loading head_library.json! Shutting down.", e);
            get_module().getServer().shutdown();
        }
    }

    // Restore correct head item from a head library when broken
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_block_break(final BlockBreakEvent event) {
        if (!config_player_head_drops) {
            return;
        }

        final var block = event.getBlock();
        if (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }

        final var skull = (Skull) block.getState();
        final var texture = texture_from_skull(skull);
        if (texture == null) {
            return;
        }

        final var head_material = HeadMaterialLibrary.from_texture(texture);
        if (head_material == null) {
            return;
        }

        // Set to air and drop item
        block.setType(Material.AIR);
        drop_naturally(block, head_material.item());
    }
}