package org.oddlama.vane.admin;

import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class HazardProtection extends Listener<Admin> {

    private WorldRebuild world_rebuild;

    @ConfigBoolean(def = true, desc = "Restrict wither spawning to a list of worlds defined by wither_world_whitelist.")
    private boolean config_enable_wither_world_whitelist;

    @ConfigStringList(
        def = { "world_nether", "world_the_end" },
        desc = "A list of worlds in which the wither may be spawned."
    )
    private List<String> config_wither_world_whitelist;

    @ConfigBoolean(def = true, desc = "Disables explosions from the wither.")
    private boolean config_disable_wither_explosions;

    @ConfigBoolean(def = true, desc = "Disables explosions from creepers.")
    private boolean config_disable_creeper_explosions;

    @ConfigBoolean(def = true, desc = "Disables enderman block pickup.")
    private boolean config_disable_enderman_block_pickup;

    @ConfigBoolean(def = true, desc = "Disables entities from breaking doors (various zombies).")
    private boolean config_disable_door_breaking;

    @ConfigBoolean(def = true, desc = "Disables fire from lightning.")
    private boolean config_disable_lightning_fire;

    @LangMessage
    private TranslatedMessage lang_wither_spawn_prohibited;

    public HazardProtection(Context<Admin> context) {
        super(
            context.group(
                "hazard_protection",
                "Enable hazard protection. The options below allow more fine-grained control over the hazards to protect from."
            )
        );
        world_rebuild = new WorldRebuild(get_context());
    }

    private boolean disable_explosion(EntityType type) {
        switch (type) {
            default:
                return false;
            case WITHER:
            case WITHER_SKULL:
                return config_disable_wither_explosions;
            case CREEPER:
                return config_disable_creeper_explosions;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_entity_explode(final EntityExplodeEvent event) {
        if (disable_explosion(event.getEntityType())) {
            if (world_rebuild.enabled()) {
                // Schedule rebuild
                world_rebuild.rebuild(event.blockList());
                // Remove all affected blocks from event
                event.blockList().clear();
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_hanging_break_by_entity(final HangingBreakByEntityEvent event) {
        if (disable_explosion(event.getRemover().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_entity_break_door(final EntityBreakDoorEvent event) {
        if (config_disable_door_breaking) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_block_ignite(final BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING && config_disable_lightning_fire) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_creature_spawn(final CreatureSpawnEvent event) {
        if (!config_enable_wither_world_whitelist) {
            return;
        }

        // Only for wither spawns
        if (event.getEntity().getType() != EntityType.WITHER) {
            return;
        }

        // Check if the world is whitelisted
        final var world = event.getEntity().getWorld();
        if (config_wither_world_whitelist.contains(world.getName())) {
            return;
        }

        lang_wither_spawn_prohibited.broadcast_world(world, world.getName());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on_entity_block_change(final EntityChangeBlockEvent event) {
        if (!config_disable_enderman_block_pickup) {
            return;
        }

        // Only for enderman events
        if (event.getEntity().getType() != EntityType.ENDERMAN) {
            return;
        }

        event.setCancelled(true);
    }
}
