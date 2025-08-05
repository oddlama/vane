package org.oddlama.vane.trifles;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicate_chest_match;
import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicate_container_match;
import static org.oddlama.vane.trifles.ItemFinderPacketUtils.indicate_entity_match;

public class ItemFinder extends Listener<Trifles> {
    @ConfigInt(
        def = 2,
        min = 1,
        max = 10,
        desc = "The radius of chunks in which containers (and possibly entities) are checked for matching items."
    )
    public int config_radius;

    @ConfigBoolean(def = true, desc = "Also search entities such as players, mobs, minecarts, ...")
    public boolean config_search_entities;

    @ConfigBoolean(
        def = false,
        desc = "Only allow players to use the shift+rightclick shortcut when they have the shortcut permission `vane.trifles.use_item_find_shortcut`."
    )
    public boolean config_require_permission;

    private static final int FALLBACK_TASK_INTERVAL = 15;

    // This permission allows players to use the shift+rightclick.
    public final Permission use_item_find_shortcut_permission;

    public ItemFinder(Context<Trifles> context) {
        super(
            context.group(
                "item_finder",
                "Enables players to search for items in nearby containers by either shift-right-clicking a similar item in their inventory or by using the `/finditem <item>` command."
            )
        );
        // Register admin permission
        use_item_find_shortcut_permission = new Permission(
            "vane." + get_module().get_name() + ".use_item_find_shortcut",
            "Allows a player to use shfit+rightclick to search for items if the require_permission config is set",
            PermissionDefault.FALSE
        );
        get_module().register_permission(use_item_find_shortcut_permission);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_player_click_inventory(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (config_require_permission && !player.hasPermission(use_item_find_shortcut_permission)) {
            return;
        }

        final var item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Shift-rightclick
        if (
            !(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClick() == ClickType.SHIFT_RIGHT)
        ) {
            return;
        }

        event.setCancelled(true);
        if (find_item(player, item.getType())) {
            get_module().schedule_next_tick(player::closeInventory);
        }
    }

    private boolean is_container(final Block block) {
        return block.getState() instanceof Container;
    }

    private void fallback_indicate_match(@NotNull Player player, @NotNull Location location) {
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.4, 0.0, 0.0, 0.0);
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.4, 0.0, 0.0);
        player.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, location, 130, 0.0, 0.0, 0.4, 0.0);
        player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, location, 70, 0.2, 0.2, 0.2, 0.0);
    }

    public boolean find_item(@NotNull final Player player, @NotNull final Material material) {
        // Find chests in configured radius and sort them.
        boolean any_found = false;
        final var world = player.getWorld();
        final var origin_chunk = player.getChunk();
        final var packet_events_enabled = get_module().packet_events_enabled;
        for (int cx = origin_chunk.getX() - config_radius; cx <= origin_chunk.getX() + config_radius; ++cx) {
            for (int cz = origin_chunk.getZ() - config_radius; cz <= origin_chunk.getZ() + config_radius; ++cz) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                final var chunk = world.getChunkAt(cx, cz);
                for (final var tile_entity : chunk.getTileEntities(this::is_container, false)) {
                    if (tile_entity instanceof Container container) {
                        if (container.getInventory().contains(material)) {
                            if (container.getType() == Material.CHEST) {
                                if (packet_events_enabled) {
                                    indicate_chest_match(get_module(), player, container);
                                } else {
                                    fallback_indicate_match(player, container.getLocation().add(0.5, 0.5, 0.5));
                                }
                            } else {
                                if (packet_events_enabled) {
                                    indicate_container_match(get_module(), player, container);
                                } else {
                                    fallback_indicate_match(player, container.getLocation().add(0.5, 0.5, 0.5));
                                }
                            }
                            any_found = true;
                        }
                    }
                }
                if (config_search_entities) {
                    for (final var entity : chunk.getEntities()) {
                        // Don't indicate the player
                        if (entity == player) {
                            continue;
                        }

                        if (entity instanceof InventoryHolder holder) {
                            if (holder.getInventory().contains(material)) {
                                if (packet_events_enabled) {
                                    indicate_entity_match(get_module(), player, entity);
                                } else {
                                    fallback_indicate_match(player, entity.getLocation());
                                }
                                any_found = true;
                            }
                        }
                    }
                }
            }
        }

        if (any_found) {
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.MASTER, 1.0f, 1.3f);
        } else {
            player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f);
        }

        return any_found;
    }
}