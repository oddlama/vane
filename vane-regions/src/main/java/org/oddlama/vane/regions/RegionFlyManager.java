package org.oddlama.vane.regions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionFlyManager extends Listener<Regions> {
    
    // Track which players are flying via the region fly feature
    // Maps player UUID -> region they were in when they enabled flying
    private Map<UUID, UUID> flying_players = new HashMap<>();
    
    // Track players who have auto-fly enabled (allowFlight but not actively flying)
    private Map<UUID, UUID> auto_fly_enabled = new HashMap<>();
    
    // Track players who are protected from fall damage after leaving a region
    private Map<UUID, Long> fall_damage_protected = new HashMap<>();

    // Track players who manually disabled flight via /fly (opt-out of auto-fly)
    private java.util.HashSet<UUID> manual_fly_opt_out = new java.util.HashSet<>();
    
    public RegionFlyManager(Context<Regions> context) {
        super(context);
    }
    
    public void add_flying_player(final UUID player_id, final Region region) {
        flying_players.put(player_id, region.id());
        // Also add to auto-fly when manually enabling
        auto_fly_enabled.put(player_id, region.id());
        // Clear manual opt-out when explicitly enabling
        manual_fly_opt_out.remove(player_id);
    }
    
    public void remove_flying_player(final UUID player_id) {
        flying_players.remove(player_id);
        auto_fly_enabled.remove(player_id);
    }

    public void set_manual_opt_out(final UUID player_id, final boolean opt_out) {
        if (opt_out) {
            manual_fly_opt_out.add(player_id);
        } else {
            manual_fly_opt_out.remove(player_id);
        }
    }

    public boolean is_manual_opt_out(final UUID player_id) {
        return manual_fly_opt_out.contains(player_id);
    }
    
    public boolean is_region_flying(final UUID player_id) {
        return flying_players.containsKey(player_id) || auto_fly_enabled.containsKey(player_id);
    }
    
    public boolean is_actively_flying(final UUID player_id) {
        return flying_players.containsKey(player_id);
    }
    
    private boolean can_fly_in_region(final Player player, final Region region) {
        if (region == null) {
            return false;
        }
        
        final var group = region.region_group(get_module());
        final var role = group.get_role(player.getUniqueId());
        
        // Check if player has BUILD permission (which friends and admins have)
        // or ADMIN permission
        return role.get_setting(RoleSetting.ADMIN) || role.get_setting(RoleSetting.BUILD);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_move(final PlayerMoveEvent event) {
        final var player = event.getPlayer();
        final var player_id = player.getUniqueId();
        
        // Skip if player is in creative or spectator mode (they can fly anyway)
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        // Only proceed when the player actually changed block coordinates
        if (!event.hasChangedBlock()) {
            return;
        }
        
        // Check current region
        final var current_region = get_module().region_at(event.getTo());
        final var old_region_id = flying_players.get(player_id);
        final var auto_fly_region_id = auto_fly_enabled.get(player_id);
        
        // Check if they can fly in the current region
        final boolean can_fly = can_fly_in_region(player, current_region);
        
        if (is_region_flying(player_id)) {
            // Player has fly enabled (either actively or auto)
            if (!can_fly) {
                // They left the region or don't have permission anymore
                disable_flying(player);
            } else if (current_region != null && 
                       old_region_id != null && 
                       !current_region.id().equals(old_region_id)) {
                // They entered a different region where they have permissions (and are actively flying)
                flying_players.put(player_id, current_region.id());
                auto_fly_enabled.put(player_id, current_region.id());
                // Update visualization to show new region boundaries
                get_module().start_visualizing_region(player_id, current_region);
            } else if (current_region != null && 
                       auto_fly_region_id != null && 
                       !current_region.id().equals(auto_fly_region_id)) {
                // Update auto-fly region tracking
                auto_fly_enabled.put(player_id, current_region.id());
                // Update visualization if they're actively flying
                if (player.isFlying()) {
                    get_module().start_visualizing_region(player_id, current_region);
                }
            }
        } else if (can_fly) {
            // Player entered a region where they can fly - enable auto-fly
            // only if they didn't manually opt-out via /fly
            if (!is_manual_opt_out(player_id)) {
                player.setAllowFlight(true);
                auto_fly_enabled.put(player_id, current_region.id());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void on_player_toggle_flight(final PlayerToggleFlightEvent event) {
        final var player = event.getPlayer();
        final var player_id = player.getUniqueId();
        
        // Skip if player is in creative or spectator mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        // Check if player has auto-fly enabled and is starting to fly
        if (event.isFlying() && auto_fly_enabled.containsKey(player_id)) {
            final var region_id = auto_fly_enabled.get(player_id);
            get_module().all_regions().stream()
                .filter(r -> r.id().equals(region_id))
                .findFirst()
                .ifPresent(region -> {
                    // Mark as actively flying
                    flying_players.put(player_id, region_id);
                    // Start visualizing the region
                    get_module().start_visualizing_region(player_id, region);
                });
        }
    }
    
    private void disable_flying(final Player player) {
        final var player_id = player.getUniqueId();
        
        // Don't disable flight for creative/spectator players
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            remove_flying_player(player_id);
            return;
        }
        
        // Check if player is high in the air
        final boolean high_in_air = is_player_high_in_air(player);
        
        // Disable flying
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Stop visualizing the region
        get_module().stop_visualizing_region(player_id);
        
        // If they're high in the air, protect them from fall damage
        if (high_in_air) {
            // Stop all downward velocity immediately to prevent momentum damage
            final var velocity = player.getVelocity();
            if (velocity.getY() < 0) {
                player.setVelocity(velocity.setY(0));
            }
            
            // Reset fall distance to prevent accumulated fall damage
            player.setFallDistance(0);
            
            // Enable fall damage protection for 15 seconds
            fall_damage_protected.put(player_id, System.currentTimeMillis());
        }
        
        // No messages - silent auto-disable
        remove_flying_player(player_id);
    }
    
    private boolean is_player_high_in_air(final Player player) {
        final var location = player.getLocation();
        final var world = location.getWorld();
        
        // Check blocks below the player
        for (int y = location.getBlockY(); y >= Math.max(world.getMinHeight(), location.getBlockY() - 10); y--) {
            final var block = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (block.getType().isSolid()) {
                // Found solid ground within 10 blocks, not high in air
                return false;
            }
        }
        
        // No solid ground within 10 blocks below
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void on_fall_damage(final EntityDamageEvent event) {
        // Only handle fall damage for players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // Only handle fall damage
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        
        final var player = (Player) event.getEntity();
        final var player_id = player.getUniqueId();
        
        // Check if player is protected from fall damage
        final var protection_time = fall_damage_protected.get(player_id);
        if (protection_time != null) {
            final long elapsed = System.currentTimeMillis() - protection_time;
            
            if (elapsed < get_module().config_fall_damage_protection_ms) {
                // Cancel fall damage
                event.setCancelled(true);
                // Remove protection after first use
                fall_damage_protected.remove(player_id);
            } else {
                // Protection expired
                fall_damage_protected.remove(player_id);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void on_player_quit(final PlayerQuitEvent event) {
        final var player_id = event.getPlayer().getUniqueId();
        // Clean up when player quits
        remove_flying_player(player_id);
        fall_damage_protected.remove(player_id);
        // Also stop visualizing
        get_module().stop_visualizing_region(player_id);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_gamemode_change(final PlayerGameModeChangeEvent event) {
        final var player = event.getPlayer();
        final var new_mode = event.getNewGameMode();
        
        // If switching to creative or spectator, remove from tracking
        // (they can fly anyway)
        if (new_mode == GameMode.CREATIVE || new_mode == GameMode.SPECTATOR) {
            remove_flying_player(player.getUniqueId());
        }
    }
}


