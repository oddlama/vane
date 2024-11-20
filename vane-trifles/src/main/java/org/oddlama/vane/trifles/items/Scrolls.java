package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.Conversions.ms_to_ticks;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;

public class Scrolls extends Listener<Trifles> {

    private Set<Scroll> scrolls = new HashSet<>();
    private Set<Material> base_materials = new HashSet<>();

    @ConfigInt(
        def = 15000,
        min = 0,
        desc = "A cooldown in milliseconds that is applied when the player takes damage (prevents combat logging). Set to 0 to allow combat logging."
    )
    private int config_damage_cooldown;

    public Scrolls(Context<Trifles> context) {
        super(context.group("scrolls", "Several scrolls that allow player teleportation, and related behavior."));
        scrolls.add(new HomeScroll(get_context()));
        scrolls.add(new UnstableScroll(get_context()));
        scrolls.add(new SpawnScroll(get_context()));
        scrolls.add(new LodestoneScroll(get_context()));
        scrolls.add(new DeathScroll(get_context()));

        // Accumulate base materials so the cooldown can be applied to all scrolls regardless of
        // base material.
        for (final var scroll : scrolls) {
            base_materials.add(scroll.baseMaterial());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false) // ignoreCancelled = false to catch right-click-air events
    public void on_player_right_click(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        // Assert this is a matching custom item
        final var player = event.getPlayer();
        final var item = player.getEquipment().getItem(event.getHand());
        final var custom_item = get_module().core.item_registry().get(item);
        if (!(custom_item instanceof Scroll scroll) || !scroll.enabled()) {
            return;
        }

        // Never actually use the base item if it's custom!
        event.setUseItemInHand(Event.Result.DENY);

        switch (event.getAction()) {
            default:
                return;
            case RIGHT_CLICK_AIR:
                break;
            case RIGHT_CLICK_BLOCK:
                // Require non-canceled state (so it won't trigger for block-actions like chests)
                // But allow if the clicked block can't be interacted with in the first place
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    final var block = event.getClickedBlock();
                    if (block.getType().isInteractable()) {
                        return;
                    }
                    event.setUseInteractedBlock(Event.Result.DENY);
                }
                break;
        }

        final var to_location = scroll.teleport_location(item, player, true);
        if (to_location == null) {
            return;
        }

        // Check cooldown
        if (player.getCooldown(scroll.baseMaterial()) > 0) {
            return;
        }

        final var current_location = player.getLocation();
        if (teleport_from_scroll(player, current_location, to_location)) {
            // Set cooldown
            cooldown_all(player, scroll.config_cooldown);

            // Damage item
            damage_item(player, item, 1);
            swing_arm(player, event.getHand());
        }
    }

    public boolean teleport_from_scroll(final Player player, final Location from, final Location to) {
        // Send scroll teleport event
        final var teleport_scroll_event = new PlayerTeleportScrollEvent(player, from, to);
        get_module().getServer().getPluginManager().callEvent(teleport_scroll_event);
        if (teleport_scroll_event.isCancelled()) {
            return false;
        }

        // Teleport
        player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);

        // Play sounds
        from.getWorld().playSound(from, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
        to.getWorld().playSound(to, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
        from.getWorld().playSound(from, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
        to.getWorld().playSound(to, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Create particles
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.0, 1.0, 0.0), 200, 1.0, 2.0, 1.0, 1.0);
        to.getWorld().spawnParticle(Particle.END_ROD, to.clone().add(0.0, 1.0, 0.0), 100, 1.0, 2.0, 1.0, 1.0);
        return true;
    }

    public void cooldown_all(final Player player, int cooldown_ms) {
        final var cooldown_ticks = (int) ms_to_ticks(cooldown_ms);
        for (final var mat : base_materials) {
            // Don't ever decrease cooldown
            if (player.getCooldown(mat) < cooldown_ticks) {
                player.setCooldown(mat, cooldown_ticks);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on_player_take_damage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cooldown_all(player, config_damage_cooldown);
        }
    }
}
